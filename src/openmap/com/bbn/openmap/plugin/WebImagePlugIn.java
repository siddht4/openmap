/* **********************************************************************
 * $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/plugin/WebImagePlugIn.java,v $
 * $Revision: 1.2 $ 
 * $Date: 2003/12/23 21:16:27 $ 
 * $Author: wjeuerle $
 *
 * Code provided by Raj Singh from Syncline, rs@syncline.com
 * Updates provided by Holger Kohler, Holger.Kohler@dsto.defence.gov.au
 * *********************************************************************
 */

package com.bbn.openmap.plugin;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.bbn.openmap.Layer;
import com.bbn.openmap.PropertyConsumer;
import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.image.ImageServerConstants;
import com.bbn.openmap.image.WMTConstants;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.plugin.*;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PropUtils;

/**
 * This class asks for an image from a web server.  How it asks for
 * that image is what is abstract.
 */
public abstract class WebImagePlugIn extends AbstractPlugIn implements ImageServerConstants {

    /** For convenience. */
    protected PlugInLayer layer = null;

    /** The last projection object received. */
    protected Projection currentProjection = null;
    
    /**
     * Create the query to be sent to the server, based on current
     * settings.
     */
    public abstract String createQueryString(Projection p);

    /**
     * The getRectangle call is the main call into the PlugIn module.
     * The module is expected to fill the graphics list with objects
     * that are within the screen parameters passed.
     *
     * @param p projection of the screen, holding scale, center
     * coords, height, width.
     */
    public OMGraphicList getRectangle(Projection p) {
        OMGraphicList list = new OMGraphicList();
	
	currentProjection = p;

        String urlString = createQueryString(p);

        if (Debug.debugging("plugin")) {
            Debug.output("WebImagePlugIn.getRectangle() with \"" + urlString + "\"");
        }

        if (urlString == null) {
            return list;
        }

        java.net.URL url = null;

        try {
            url = new java.net.URL(urlString);
            java.net.HttpURLConnection urlc =
            (java.net.HttpURLConnection)url.openConnection();

            if (Debug.debugging("plugin")) {
                Debug.output("url content type: "+ urlc.getContentType());
            }

	    if (urlc == null || urlc.getContentType() == null) {
		if (layer != null) {
		    layer.fireRequestMessage(getName() + ": unable to connect to " + getServerName());
		} else {
		    Debug.error(getName() + ": unable to connect to " + getServerName());
		}
		return list;
	    }

            // text
            if (urlc.getContentType().startsWith("text")) {
                java.io.BufferedReader bin = new java.io.BufferedReader(
                    new java.io.InputStreamReader(urlc.getInputStream())
                );
                String st;
                String message = "";
                while ((st=bin.readLine()) != null) {
                    message += st;
                }

//                  Debug.error(message);
		// How about we toss the message out to the user instead?
		if (layer != null) {
		    layer.fireRequestMessage(message);
		}

            // image
            } else if (urlc.getContentType().startsWith("image")) {
                urlc.disconnect();
                ImageIcon ii = new ImageIcon(url);
                OMRaster image = new OMRaster((int)0, (int)0, ii);
                list.add(image);
            } // end if image
        } catch (java.net.MalformedURLException murle) {
            Debug.error("WebImagePlugIn: URL \"" + urlString +
			"\" is malformed.");
        } catch (java.io.IOException ioe) {
	    messageWindow.showMessageDialog(null, getName() + ":\n\n   Couldn't connect to " + getServerName(), "Connection Problem", JOptionPane.INFORMATION_MESSAGE);

        }

        list.generate(p);
        return list;
    } //end getRectangle

    public abstract String getServerName();

    public java.awt.Component getGUI() {
	JPanel panel = new JPanel(new GridLayout(0, 1));
	JButton parameterButton = new JButton("Adjust Parameters");
	parameterButton.setActionCommand(Layer.DisplayPropertiesCmd);

	if (layer != null) {
	    parameterButton.addActionListener(layer);
	}

	JButton viewQueryButton = new JButton("View Current Query");
	viewQueryButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    if (layer != null) {
			String query = createQueryString(currentProjection);
			Vector queryStrings = PropUtils.parseMarkers(query, "&");
			StringBuffer updatedQuery = new StringBuffer();
			Iterator it = queryStrings.iterator();
			if (it.hasNext()) {
			    updatedQuery.append((String)it.next());
			}
			while (it.hasNext()) {
			    updatedQuery.append("&\n   ");
			    updatedQuery.append((String) it.next());
			}

			messageWindow.showMessageDialog(null, updatedQuery.toString(), "Current Query for " + getName(), JOptionPane.INFORMATION_MESSAGE);
		    }
		}
	    });

	redrawButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
		    if (layer != null) {
			layer.doPrepare();
		    }
		}
	    });

	redrawButton.setEnabled(layer != null);

	panel.add(parameterButton);
	panel.add(viewQueryButton);
	panel.add(redrawButton);
	return panel;
    }

    protected JButton redrawButton = new JButton("Query Server");
    protected JOptionPane messageWindow = new JOptionPane();

    /** 
     * Set the component that this PlugIn uses as a grip to the map.  
     */
    public void setComponent(Component comp) {
	super.setComponent(comp);
	if (comp instanceof PlugInLayer) {
	    layer = (PlugInLayer) comp;
	} else {
	    layer = null;
	}
	redrawButton.setEnabled(layer != null);
    }

} //end WMSPlugin
