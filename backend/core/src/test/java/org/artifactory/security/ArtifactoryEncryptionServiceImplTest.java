package org.artifactory.security;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jfrog.security.crypto.EncryptionWrapper;
import org.jfrog.security.crypto.EncryptionWrapperFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Noam Shemesh
 */
public class ArtifactoryEncryptionServiceImplTest {

    @Test
    public void testEncryption() throws IOException {
        File masterKeyFile = File.createTempFile("master", "key");
        FileUtils.writeStringToFile(masterKeyFile, "JRMP38RoQxYq6m8DL5aZejrTNjiYVNxeWUqcEPh2UTxGHrxNmT6K9YdwWDaXjdGNoYwSqBt54Krp97ZvMTiqNSRLi4BQwUpWePX7Damie8fxqzK97zuCdWjPCXNE4n3SMryha3uMQtSmpAL6G1SAYXVB222hAtczeZJhb1cRDY3gFcRZve3TTzmxYhYsmtZaaUGFKEtnfmL9MUmxfy1J3gSQj7rUS1e4YkciEP59M5EwYxDuaeymPikDmyDb1New8J6MKEF4JVHbcSXtSkhwk6nLg5DYyXdBha4jaLdexiFfV3hz6g3hzVXmv11qfjLZWUy9NaHMbirHzavqFzzTHjEE2fNELVUEmCcsPNFEBeFBCz4VPGaCiEw3d1DYCAL5NwwHQykyt6TkK4VPoasuueyK84McLGcsLbcXvCcrBqGqFSQK8autJt486YMAXmsurcxyLe2bFgXPKodheFuPfyWVf\n" +
                "JUHfDLxBPMe4YZbWLKdbams2ZTPq3rmG1zxgjCVxHqpjXALvJ77V1afbwQgwTqSjqQD3szfzZrpK46ctsTzqqnBEQEoXr8SM8Xyu124CjHtuYX45XNiSP3ZgCjH3wkA2jKAn6");

        EncryptionWrapper masterWrapper = EncryptionWrapperFactory
                .createMasterWrapper(masterKeyFile, masterKeyFile.getParentFile(),
                        1, FileFilterUtils.trueFileFilter());

        System.out.println(masterWrapper.decryptIfNeeded("AM65WGmS9TQLFND5pi77cVfY4UDHNqMoMz3Wgc").getDecryptedData());
    }

}