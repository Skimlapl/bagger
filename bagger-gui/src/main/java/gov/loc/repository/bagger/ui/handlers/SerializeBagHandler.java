
package gov.loc.repository.bagger.ui.handlers;

import gov.loc.repository.bagger.bag.impl.DefaultBag;
import gov.loc.repository.bagger.ui.BagView;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.application.ApplicationPage;
import org.springframework.richclient.application.PageComponent;

public class SerializeBagHandler extends AbstractAction {
	private static final Log log = LogFactory.getLog(SerializeBagHandler.class);
   	private static final long serialVersionUID = 1L;
	BagView bagView;
	DefaultBag bag;

	public SerializeBagHandler(BagView bagView) {
		super();
		this.bagView = bagView;
	}

	public void actionPerformed(ActionEvent e) {
		this.bag = bagView.getBag();

		JRadioButton cb = (JRadioButton)e.getSource();
        boolean isSel = cb.isSelected();
        if (isSel) {
        	if (cb == bagView.noneButton) {
            	bag.setIsSerial(false);
            	bag.setSerialMode(DefaultBag.NO_MODE);
        	} else if (cb == bagView.zipButton) {
            	bag.setIsSerial(true);
            	bag.setSerialMode(DefaultBag.ZIP_MODE);
        	} else if (cb == bagView.tarButton) {
            	bag.setIsSerial(true);
            	bag.setSerialMode(DefaultBag.TAR_MODE);
        	} else {
            	bag.setIsSerial(false);
            	bag.setSerialMode(DefaultBag.NO_MODE);
        	}
        }
        bagView.setBag(bag);
	}
}