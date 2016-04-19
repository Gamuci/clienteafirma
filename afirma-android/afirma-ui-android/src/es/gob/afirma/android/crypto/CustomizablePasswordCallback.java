package es.gob.afirma.android.crypto;

import javax.security.auth.callback.PasswordCallback;

/** <i>PasswordCallBack</i> que permite establecer al vuelo un mensaje de solicitud en el momento de
 * pedir la contrase&ntilde;a al usuario.
 * @author Alberto Mart&iacute;nez */
public abstract class CustomizablePasswordCallback extends PasswordCallback {

    /** Serial version UID */
    private static final long serialVersionUID = -6307987785481122679L;

    /** Construye un <code>PasswordCallback</code> con un <i>prompt</i> y un booleano especificando si
     * la contrase&ntilde;a debe ser mostrada mientras se escribe.
     * @param prompt El texto a mostrar para pedir la contrase&ntilde;a
     * @param echoOn Verdadero si la contrase&ntilde;a deber&iacute;a ser mostrada mientras se escribe */
    public CustomizablePasswordCallback(final String prompt, final boolean echoOn) {
        super(prompt, echoOn);
    }

    /** Solicita al usuario una conrtrase&ntilde;a.
     * @param prompt Texto de solicitud
     * @return Contrase&ntilde;a introducida por el usuario o <code>null</code> si cancel&oacute; la introducci&oacute;n */
    public abstract char[] getPassword(final String prompt);
}