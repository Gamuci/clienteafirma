/*
 * Este fichero forma parte del Cliente @firma.
 * El Cliente @firma es un applet de libre distribucion cuyo codigo fuente puede ser consultado
 * y descargado desde www.ctt.map.es.
 * Copyright 2009,2010 Ministerio de la Presidencia, Gobierno de Espana
 * Este fichero se distribuye bajo licencia GPL version 3 segun las
 * condiciones que figuran en el fichero 'licence' que se acompana.  Si se   distribuyera este
 * fichero individualmente, deben incluirse aqui las condiciones expresadas alli.
 */
package es.gob.afirma.ui.principal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.Vector;
import java.util.logging.Logger;

import javax.security.auth.callback.PasswordCallback;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.Caret;

import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.envelopers.cms.AOCMSEnveloper;
import es.gob.afirma.envelopers.cms.CMSDecipherAuthenticatedEnvelopedData;
import es.gob.afirma.envelopers.cms.CMSDecipherEnvelopData;
import es.gob.afirma.envelopers.cms.CMSDecipherSignedAndEnvelopedData;
import es.gob.afirma.keystores.main.callbacks.NullPasswordCallback;
import es.gob.afirma.keystores.main.common.AOKeyStore;
import es.gob.afirma.keystores.main.common.AOKeyStoreManager;
import es.gob.afirma.keystores.main.common.AOKeyStoreManagerFactory;
import es.gob.afirma.keystores.main.common.AOKeystoreAlternativeException;
import es.gob.afirma.keystores.main.common.KeyStoreConfiguration;
import es.gob.afirma.keystores.main.filters.CertificateFilter;
import es.gob.afirma.ui.listeners.ElementDescriptionFocusListener;
import es.gob.afirma.ui.listeners.ElementDescriptionMouseListener;
import es.gob.afirma.ui.utils.ConfigureCaret;
import es.gob.afirma.ui.utils.CustomDialog;
import es.gob.afirma.ui.utils.ExtFilter;
import es.gob.afirma.ui.utils.GeneralConfig;
import es.gob.afirma.ui.utils.HelpUtils;
import es.gob.afirma.ui.utils.KeyStoreLoader;
import es.gob.afirma.ui.utils.Messages;
import es.gob.afirma.ui.utils.RequestFocusListener;
import es.gob.afirma.ui.utils.SelectionDialog;
import es.gob.afirma.ui.utils.UIPasswordCallbackAccessibility;
import es.gob.afirma.ui.utils.Utils;

/** Clase que se encarga de desensobrar el contenido de un fichero. */
final class Desensobrado extends JPanel {

    static Logger logger = Logger.getLogger(Desensobrado.class.getName());

    private static final long serialVersionUID = 1L;

    /** Creates new form desensobrado */
    public Desensobrado() {
        initComponents();
    }

    /** Carga el combo almacen respecto al sistema operativo en el que se encuentra
     * la aplicaci&oacute;
     * @param comboAlmacen Combo donde se cargan los tipos de almacen */
    private static void cargarComboAlmacen(final JComboBox comboAlmacen) {
        comboAlmacen.setModel(new DefaultComboBoxModel(KeyStoreLoader.getKeyStoresToSign()));
    }

    /** Pulsar boton examinar: Muestra una ventana para seleccinar un archivo.
     * Modifica el valor de la caja con el nombre del archivo seleccionado
     * @param campoFichero Campo en el que se escribe el nombre del fichero seleccionado */
    void examinarActionPerformed(final JTextField campoFichero) {
        final File selectedFile = SelectionDialog.showFileOpenDialog(this, Messages.getString("Seleccione.fichero.desensobrar")); //$NON-NLS-1$
        if (selectedFile != null) {
            campoFichero.setText(selectedFile.getAbsolutePath());
        }
    }

    /** Pulsar boton extraer: Extrae la informacion del sobre
     * @param comboAlmacen Combo con el almacen de claves
     * @param campoFichero Campo con el nombre del fichero a extraer
     * @param checkIniciar Checkbox que indica si los datos se deben de iniciar */
    void extraerActionPerformed(final JComboBox comboAlmacen, final JTextField campoFichero, final JCheckBox checkIniciar) {
        // Obtenemos la ruta del sobre
        final String envelopPath = campoFichero.getText();
        if (envelopPath == null || envelopPath.equals("") || !new File(envelopPath).exists() || !new File(envelopPath).isFile()) { //$NON-NLS-1$
            CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                           true,
                                           Messages.getString("Desensobrado.msg.erro.fichero"), Messages.getString("Desensobrado.msg.titulo"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            campoFichero.requestFocusInWindow(); // Foco al campo que contiene el path al fichero
        }
        else {
            // Mensaje que indica que se va a realizar el proceso de firma y que puede llevar un tiempo
            CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                           true,
                                           Messages.getString("Firma.msg.info"), Messages.getString("PrincipalGUI.TabConstraints.tabTitleDesensobrado"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$

            byte[] envelopData = null;
            try {
                final FileInputStream envelopFis = new FileInputStream(new File(envelopPath));
                envelopData = AOUtil.getDataFromInputStream(envelopFis);
            }
            catch (final Exception e) {
                logger.severe("No se ha encontrado o no se ha podido leer el fichero: " + envelopPath); //$NON-NLS-1$
                CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                               true,
                                               Messages.getString("Desensobrado.msg.error.fichero2"), "Error", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return;
            }

            // Se carga el almacen y el certificado
            PrivateKeyEntry privateKeyEntry = null;
            try {
                final AOKeyStoreManager keyStoreManager = getKeyStoreManager((KeyStoreConfiguration) comboAlmacen.getSelectedItem());
                privateKeyEntry = getPrivateKeyEntry(keyStoreManager, comboAlmacen);
            }
            catch (final AOCancelledOperationException e) {
                logger.info("Operacion cancelada por el usuario"); //$NON-NLS-1$
                return;
            }
            catch (final KeyException e) {
                // Control de la excepcion generada al introducir mal la contrasena para el almacen
                CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                               true,
                                               Messages.getString("Desensobrado.msg.error.contrasenia"), Messages.getString("error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            catch (final AOKeystoreAlternativeException e) {
                CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                               true,
                                               Messages.getString("Desensobrado.msg.error.almacen.contrasenia"), Messages.getString("error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            catch (final AOException e) {
                logger.severe("Error al abrir el almacen de claves del usuario: " + e); //$NON-NLS-1$
                CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                               true,
                                               Messages.getString("Desensobrado.msg.error.almacen"), Messages.getString("error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            catch (final Exception e) {
                logger.severe("Error al recuperar el certificado del usuario: " + e); //$NON-NLS-1$
                CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                               true,
                                               Messages.getString("Desensobrado.msg.error.certificado"), Messages.getString("error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            // Identificamos el tipo de envoltorio y recuperamos los datos
            byte[] recoveredData = null;
            try {
                final AOCMSEnveloper enveloper = new AOCMSEnveloper();
                // EnvelopedData
                if (enveloper.isCMSValid(envelopData, AOSignConstants.CMS_CONTENTTYPE_ENVELOPEDDATA)) {
                    recoveredData = new CMSDecipherEnvelopData().dechiperEnvelopData(envelopData, privateKeyEntry);
                    // SignedAndEnvelopedData
                }
                else if (enveloper.isCMSValid(envelopData, AOSignConstants.CMS_CONTENTTYPE_SIGNEDANDENVELOPEDDATA)) {
                    recoveredData = new CMSDecipherSignedAndEnvelopedData().dechiperSignedAndEnvelopData(envelopData, privateKeyEntry);
                    // AuthenticatedAndEnvelopedData
                }
                else if (enveloper.isCMSValid(envelopData, AOSignConstants.CMS_CONTENTTYPE_AUTHENVELOPEDDATA)) {
                    recoveredData = new CMSDecipherAuthenticatedEnvelopedData().dechiperAuthenticatedEnvelopedData(envelopData, privateKeyEntry);
                    // Envoltorio no reconocido
                }
                else {
                    CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                                   true,
                                                   Messages.getString("Desensobrado.msg.error.sobre"), Messages.getString("error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
            }
            catch (final AOException e) {
                logger.severe("Error al abrir el sobre digital: " + e); //$NON-NLS-1$
                // El pop-up muestra el mensaje de la excepcion
                CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                               true,
                                               e.getMessage(),
                                               Messages.getString("error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return;
            }
            catch (final Exception e) {
                logger.severe("Error al abrir el sobre digital: " + e); //$NON-NLS-1$
                // El pop-up muestra el mensaje de la excepcion
                CustomDialog.showMessageDialog(SwingUtilities.getRoot(this),
                                               true,
                                               e.getMessage(),
                                               Messages.getString("error"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                return;
            }

            // Quitamos la extension del nombre de fichero y establecemos su nombre como el por defecto
            String name = new File(envelopPath).getName();
            if (name.lastIndexOf('.') != -1) {
                name = name.substring(0, name.lastIndexOf('.'));
            }

            // Salvamos los datos
            final File file = SelectionDialog.saveDataToFile(Messages.getString("Desensobrado.filechooser.save.title"), recoveredData, name, null, this); //$NON-NLS-1$
            if (file != null && checkIniciar.isSelected()) {
                Utils.openFile(file);
            }
        }
    }

    private AOKeyStoreManager getKeyStoreManager(final KeyStoreConfiguration ksConfiguration) throws AOException,
    InvalidKeyException,
    AOKeystoreAlternativeException,
    AOCancelledOperationException {
        PasswordCallback pssCallback;
        final AOKeyStore store = ksConfiguration.getType();
        String lib = ksConfiguration.getLib();
        if (store == AOKeyStore.WINDOWS || store == AOKeyStore.WINROOT) {
            pssCallback = new NullPasswordCallback();
        }
        else if (store == AOKeyStore.PKCS12) {
            pssCallback =
                new UIPasswordCallbackAccessibility(Messages.getString("Msg.pedir.contraenia") + " " + store.getDescription(), SwingUtilities.getRoot(this), //$NON-NLS-1$
                                                    Messages.getString("CustomDialog.showInputPasswordDialog.title"), KeyEvent.VK_O, Messages.getString("CustomDialog.showInputPasswordDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
            final File selectedFile =
                SelectionDialog.showFileOpenDialog(this, Messages.getString("Open.repository"), (ExtFilter) Utils.getRepositoryFileFilter()); //$NON-NLS-1$
            if (selectedFile != null) {
                lib = selectedFile.getAbsolutePath();
            }
            else {
                throw new AOCancelledOperationException();
            }

        }
        else {
            pssCallback =
                new UIPasswordCallbackAccessibility(Messages.getString("Msg.pedir.contraenia") + " " + store.getDescription(), SwingUtilities.getRoot(this), //$NON-NLS-1$
                                                    Messages.getString("CustomDialog.showInputPasswordDialog.title"), KeyEvent.VK_O, Messages.getString("CustomDialog.showInputPasswordDialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        try {
            return AOKeyStoreManagerFactory.getAOKeyStoreManager(store, lib, ksConfiguration.toString(), pssCallback, this);
        }
        catch (final AOCancelledOperationException e) {
            throw e;
        }
        catch (final InvalidKeyException e) {
            throw e;
        }
        catch (final AOKeystoreAlternativeException e) {
            throw e;
        }
        catch (final Exception e) {
            throw new AOException("Error al inicializar el almacen", e); //$NON-NLS-1$
        }
    }

    private PrivateKeyEntry getPrivateKeyEntry(final AOKeyStoreManager keyStoreManager, final JComboBox comboAlmacen) throws AOException {
        // Seleccionamos un certificado
        final String selectedcert =
            Utils.showCertSelectionDialog(keyStoreManager.getAliases(),
                                          keyStoreManager,
                                          SwingUtilities.getRoot(this),
                                          true,
                                          true,
                                          true,
                                          new Vector<CertificateFilter>(0),
                                          false);

        // Comprobamos si se ha cancelado la seleccion
        if (selectedcert == null) {
            throw new AOCancelledOperationException("Operacion de firma cancelada por el usuario"); //$NON-NLS-1$
        }

        // Recuperamos la clave del certificado
        PrivateKeyEntry privateKeyEntry = null;
        try {
            privateKeyEntry =
                keyStoreManager.getKeyEntry(selectedcert,
                                            Utils.getCertificatePC(((KeyStoreConfiguration) comboAlmacen.getSelectedItem()).getType(),
                                                                   SwingUtilities.getRoot(this)));
        }
        catch (final AOCancelledOperationException e) {
            // Si se ha cancelado la operacion lo informamos en el nivel superior para que se trate.
            // Este relanzamiento se realiza para evitar la siguiente captura generica de excepciones
            // que las relanza en forma de AOException
            throw e;
        }
        catch (final Exception e) {
            logger.severe("No se ha podido obtener el certicado con el alias '" + selectedcert + "': " + e); //$NON-NLS-1$ //$NON-NLS-2$
            throw new AOException(e.getMessage());
        }
        return privateKeyEntry;
    }

    /** Inicializacion de los componentes */
    private void initComponents() {
        setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(13, 13, 0, 13);
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.gridx = 0;

        // Etiqueta sobre digital a abrir
        final JLabel etiquetaFichero = new JLabel();
        etiquetaFichero.setText(Messages.getString("Desensobrado.buscar")); // NOI18N //$NON-NLS-1$
        Utils.setContrastColor(etiquetaFichero);
        Utils.setFontBold(etiquetaFichero);
        add(etiquetaFichero, c);

        c.insets = new Insets(0, 13, 0, 0);
        c.gridwidth = 1;
        c.gridy = 1;

        // Campo con el nombre del archivo a extraer
        final JTextField campoFichero = new JTextField();
        campoFichero.setToolTipText(Messages.getString("Desensobrado.buscar.caja.description")); // NOI18N //$NON-NLS-1$
        campoFichero.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar,
                                                                          Messages.getString("Desensobrado.buscar.caja.description.status"))); //$NON-NLS-1$
        campoFichero.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar,
                                                                          Messages.getString("Desensobrado.buscar.caja.description.status"))); //$NON-NLS-1$
        campoFichero.getAccessibleContext().setAccessibleName(etiquetaFichero.getText() + " ALT + O."); // NOI18N
        campoFichero.getAccessibleContext().setAccessibleDescription(Messages.getString("Desensobrado.buscar.caja.description")); // NOI18N //$NON-NLS-1$
        campoFichero.addAncestorListener(new RequestFocusListener(false));
        if (GeneralConfig.isBigCaret()) {
            final Caret caret = new ConfigureCaret();
            campoFichero.setCaret(caret);
        }
        Utils.remarcar(campoFichero);
        Utils.setFontBold(campoFichero);
        add(campoFichero, c);

        // Relacion entre etiqueta y campo de texto
        etiquetaFichero.setLabelFor(campoFichero);
        // Asignacion de mnemonico
        etiquetaFichero.setDisplayedMnemonic(KeyEvent.VK_O);

        c.insets = new Insets(0, 10, 0, 13);
        c.weightx = 0.0;
        c.gridx = 1;

        final JPanel panelExaminar = new JPanel(new GridLayout(1, 1));
        // Boton examinar
        final JButton examinar = new JButton();
        examinar.setMnemonic(KeyEvent.VK_E);
        examinar.setText(Messages.getString("PrincipalGUI.Examinar")); // NOI18N //$NON-NLS-1$
        examinar.setToolTipText(Messages.getString("PrincipalGUI.Examinar.description")); // NOI18N //$NON-NLS-1$
        examinar.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar,
                                                                      Messages.getString("PrincipalGUI.Examinar.description.status"))); //$NON-NLS-1$
        examinar.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar,
                                                                      Messages.getString("PrincipalGUI.Examinar.description.status"))); //$NON-NLS-1$
        examinar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                examinarActionPerformed(campoFichero);
            }
        });
        // examinar.getAccessibleContext().setAccessibleName(examinar.getText() + " " + Messages.getString("PrincipalGUI.Examinar.description.status")
        // ); // NOI18N
        examinar.getAccessibleContext().setAccessibleDescription(Messages.getString("PrincipalGUI.Examinar.description")); // NOI18N //$NON-NLS-1$
        Utils.remarcar(examinar);
        Utils.setContrastColor(examinar);
        Utils.setFontBold(examinar);

        panelExaminar.add(examinar);
        add(panelExaminar, c);

        // Espacio en blanco
        final JPanel emptyPanel01 = new JPanel();
        emptyPanel01.setPreferredSize(new Dimension(1, 1));
        c.weightx = 1.0;
        c.weighty = 0.2;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(0, 0, 0, 0);
        add(emptyPanel01, c);

        c.insets = new Insets(13, 13, 0, 13);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 3;

        // Etiqueta almacen o repositorio
        final JLabel etiquetaAlmacen = new JLabel();
        etiquetaAlmacen.setText(Messages.getString("Desensobrado.almacen")); // NOI18N //$NON-NLS-1$
        Utils.setContrastColor(etiquetaAlmacen);
        Utils.setFontBold(etiquetaAlmacen);
        add(etiquetaAlmacen, c);

        c.insets = new Insets(0, 13, 0, 13);
        c.gridy = 4;
        c.weighty = 0.1;
        c.fill = GridBagConstraints.BOTH;

        // Combo con el almacen o repositorio de certificados
        final JComboBox comboAlmacen = new JComboBox();
        comboAlmacen.setToolTipText(Messages.getString("Desensobrado.almacen.combo.description")); // NOI18N //$NON-NLS-1$
        comboAlmacen.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar,
                                                                          Messages.getString("Desensobrado.almacen.combo.description.status"))); //$NON-NLS-1$
        comboAlmacen.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar,
                                                                          Messages.getString("Desensobrado.almacen.combo.description.status"))); //$NON-NLS-1$
        // comboAlmacen.getAccessibleContext().setAccessibleName(etiquetaAlmacen.getText()+ " " +
        // Messages.getString("Desensobrado.almacen.combo.description.status") + " ALT + A."); // NOI18N
        comboAlmacen.getAccessibleContext().setAccessibleDescription(Messages.getString("Desensobrado.almacen.combo.description")); // NOI18N //$NON-NLS-1$
        cargarComboAlmacen(comboAlmacen);
        Utils.remarcar(comboAlmacen);
        Utils.setContrastColor(comboAlmacen);
        Utils.setFontBold(comboAlmacen);
        add(comboAlmacen, c);

        // Relacion entre etiqueta y combo
        etiquetaAlmacen.setLabelFor(comboAlmacen);
        // Asignacion de mnemonico
        etiquetaAlmacen.setDisplayedMnemonic(KeyEvent.VK_A);

        // Espacio en blanco
        final JPanel emptyPanel02 = new JPanel();
        emptyPanel02.setPreferredSize(new Dimension(1, 1));
        c.weightx = 1.0;
        c.weighty = 0.2;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 5;
        c.insets = new Insets(0, 0, 0, 0);
        add(emptyPanel02, c);

        c.insets = new Insets(13, 13, 0, 13);
        c.weightx = 1.0;
        c.gridy = 6;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;

        // Etiqueta con las opciones de apertura
        final JLabel etiquetaOpciones = new JLabel();
        etiquetaOpciones.setText(Messages.getString("Desensobrado.opciones")); // NOI18N //$NON-NLS-1$
        Utils.setContrastColor(etiquetaOpciones);
        Utils.setFontBold(etiquetaOpciones);
        add(etiquetaOpciones, c);

        c.insets = new Insets(0, 13, 0, 13);
        c.gridy = 7;

        final JPanel panelCheckIniciar = new JPanel(new GridLayout(1, 1));
        panelCheckIniciar.getAccessibleContext().setAccessibleName(Messages.getString("Desensobrado.opciones")); //$NON-NLS-1$
        // Checkbox para iniciar el contenido
        final JCheckBox checkIniciar = new JCheckBox();
        checkIniciar.setText(Messages.getString("Desensobrado.check")); // NOI18N //$NON-NLS-1$
        checkIniciar.setToolTipText(Messages.getString("Desensobrado.check.check.description")); // NOI18N //$NON-NLS-1$
        checkIniciar.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar,
                                                                          Messages.getString("Desensobrado.check.check.description.status"))); //$NON-NLS-1$
        checkIniciar.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar,
                                                                          Messages.getString("Desensobrado.check.check.description.status"))); //$NON-NLS-1$
        // checkIniciar.getAccessibleContext().setAccessibleName(Messages.getString("Desensobrado.check.check") + " "
        // +Messages.getString("Desensobrado.check.check.description.status")); // NOI18N
        checkIniciar.getAccessibleContext().setAccessibleDescription(Messages.getString("Desensobrado.check.check.description")); // NOI18N //$NON-NLS-1$
        checkIniciar.setMnemonic(KeyEvent.VK_R); // Se asigna un atajo
        Utils.remarcar(checkIniciar);
        Utils.setContrastColor(checkIniciar);
        Utils.setFontBold(checkIniciar);

        panelCheckIniciar.add(checkIniciar);
        add(panelCheckIniciar, c);

        c.weighty = 1.0;
        c.gridy = 8;
        c.gridheight = 4;

        // Panel vacio para alinear el boton de aceptar en la parte de abajo de la pantalla
        final JPanel emptyPanel = new JPanel();
        add(emptyPanel, c);

        // Panel con los botones
        final JPanel panelBotones = new JPanel(new GridBagLayout());

        final GridBagConstraints cons = new GridBagConstraints();
        cons.anchor = GridBagConstraints.FIRST_LINE_START; // control de la orientacion de componentes al redimensionar
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.ipadx = 15;
        cons.gridx = 0;

        // Etiqueta para rellenar a la izquierda
        final JLabel label = new JLabel();
        panelBotones.add(label, cons);

        final JPanel panelExtraer = new JPanel(new GridLayout(1, 1));
        // Boton extraer
        final JButton extraer = new JButton();
        extraer.setMnemonic(KeyEvent.VK_X);
        extraer.setText(Messages.getString("Desensobrado.btnDescifrar")); // NOI18N //$NON-NLS-1$
        extraer.setToolTipText(Messages.getString("Desensobrado.btnDescifrar.description")); // NOI18N //$NON-NLS-1$
        extraer.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar,
                                                                     Messages.getString("Desensobrado.btnDescifrar.description.status"))); //$NON-NLS-1$
        extraer.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar,
                                                                     Messages.getString("Desensobrado.btnDescifrar.description.status"))); //$NON-NLS-1$
        extraer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                extraerActionPerformed(comboAlmacen, campoFichero, checkIniciar);
            }
        });
        // extraer.getAccessibleContext().setAccessibleName(extraer.getText() + " " +
        // Messages.getString("Desensobrado.btnDescifrar.description.status")); // NOI18N
        extraer.getAccessibleContext().setAccessibleDescription(Messages.getString("Desensobrado.btnDescifrar.description")); // NOI18N //$NON-NLS-1$
        Utils.remarcar(extraer);
        Utils.setContrastColor(extraer);
        Utils.setFontBold(extraer);

        panelExtraer.add(extraer);
        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(panelExtraer, BorderLayout.CENTER);

        cons.ipadx = 0;
        cons.gridx = 1;
        cons.weightx = 1.0;

        panelBotones.add(buttonPanel, cons);

        final JPanel panelAyuda = new JPanel();
        // Boton de ayuda
        final JButton botonAyuda = HelpUtils.helpButton("desensobrado");
        botonAyuda.setName("helpButton");

        cons.ipadx = 15;
        cons.weightx = 0.0;
        cons.gridx = 2;

        panelAyuda.add(botonAyuda);
        panelBotones.add(panelAyuda, cons);

        c.gridwidth = 2;
        c.insets = new Insets(13, 13, 13, 13);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridy = 12;

        add(panelBotones, c);

        // Accesos rapidos al menu de ayuda
        HelpUtils.enableHelpKey(campoFichero, "desensobrado.sobre"); //$NON-NLS-1$
        HelpUtils.enableHelpKey(examinar, "desensobrado.sobre"); //$NON-NLS-1$
        HelpUtils.enableHelpKey(comboAlmacen, "desensobrado.almacen"); //$NON-NLS-1$
        HelpUtils.enableHelpKey(checkIniciar, "desensobrado.iniciar"); //$NON-NLS-1$
    }
}
