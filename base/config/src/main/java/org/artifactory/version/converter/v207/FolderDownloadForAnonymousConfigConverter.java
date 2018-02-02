package org.artifactory.version.converter.v207;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds 'enabledForAnonymous' under the Folder Download config section with default value false
 *
 * @author Rotem Kfir
 */
public class FolderDownloadForAnonymousConfigConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(FolderDownloadForAnonymousConfigConverter.class);

    @Override
    public void convert(Document doc) {
        log.info("Starting to add 'Enable Folder Download For Anonymous Access' config conversion");
        Element rootElement = doc.getRootElement();
        Element folderDownloadConfig = rootElement.getChild("folderDownloadConfig", rootElement.getNamespace());
        Element enabledForAnonymous = folderDownloadConfig.getChild("enabledForAnonymous", folderDownloadConfig.getNamespace());
        if (enabledForAnonymous == null) {
            log.info("No enabledForAnonymous config found - adding default one");
            enabledForAnonymous = new Element("enabledForAnonymous", folderDownloadConfig.getNamespace()).setText("false");

            int enabledLocation = findLastLocation(folderDownloadConfig, "enabled");
            folderDownloadConfig.addContent(enabledLocation + 1, new Text("\n        "));
            folderDownloadConfig.addContent(enabledLocation + 2, enabledForAnonymous);
        }
        log.info("Finished to add 'Enable Folder Download For Anonymous Access' config conversion");
    }
}
