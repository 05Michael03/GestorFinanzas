/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.gestorfinanzas;

import javax.swing.SwingUtilities;
import gestorfinanzas.LoginFrame;

/**
 * Launcher compatibility class. Starts the Swing UI.
 */
public class GestorFinanzas {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
