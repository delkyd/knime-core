/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.ImageObserver;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LoadingPanel extends JPanel implements ImageObserver {
    
    /**
     * Displays "loading port content".
     */
    public LoadingPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        Box centerBox = Box.createHorizontalBox();
        centerBox.add(Box.createHorizontalGlue());
        centerBox.add(new JLabel("Loading port content..."));
        centerBox.add(Box.createHorizontalGlue());
        add(centerBox);
    }

    
}
