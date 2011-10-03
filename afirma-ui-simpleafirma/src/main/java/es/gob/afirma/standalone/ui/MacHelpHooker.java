/*******************************************************************************
 * Este fichero forma parte del Cliente @firma.
 * El Cliente @firma es un aplicativo de libre distribucion cuyo codigo fuente puede ser consultado
 * y descargado desde http://forja-ctt.administracionelectronica.gob.es/
 * Copyright 2009,2010,2011 Gobierno de Espana
 * Este fichero se distribuye bajo  bajo licencia GPL version 2  segun las
 * condiciones que figuran en el fichero 'licence' que se acompana. Si se distribuyera este
 * fichero individualmente, deben incluirse aqui las condiciones expresadas alli.
 ******************************************************************************/

package es.gob.afirma.standalone.ui;

import java.util.logging.Logger;

import es.gob.afirma.core.misc.Platform;

/** Clase de enlace con la ayuda nativa de Mac OS X.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s
 */
public final class MacHelpHooker {
    
    private static boolean loaded = false;
    
    static {
        if (Platform.OS.MACOSX.equals(Platform.getOS())) {
            try {
                System.loadLibrary("JavaHelpHook"); //$NON-NLS-1$
                loaded = true;
            }
            catch(final UnsatisfiedLinkError e) {
                Logger.getLogger("es.gob.afirma").warning("No se encuentra la biblioteca nativa de apertura de Apple Help: " + e);  //$NON-NLS-1$//$NON-NLS-2$
            }
            catch(final Exception e) {
                Logger.getLogger("es.gob.afirma").warning("No ha sido posible cargar la biblioteca nativa de apertura de Apple Help: " + e);  //$NON-NLS-1$//$NON-NLS-2$
            }
        }
    }
    
    /** Muestra la ayuda de la aplicaci&oacute;n en Mac OS X con formato Apple Help (que debe estar declara en el
     * <code>Info.plist</code> del empaquetado <code>.app</code>). */
    public static native void showHelp();
    
    /** Indica si es posible mostrar la ayuda nativa Apple Help.
     * @return <code>true</code> si se detecta Mac OS X y la biblioteca nativa de enlace (<code>JavaHelpHook.jnilib</code>)
     *         est&aacute; disponible, <code>false</code> en caso contrario
     */
    public static boolean isMacHelpAvailable() {
        return loaded;
    }

}
