//package org.artifactory.rest.resources.system;
//
//import com.google.common.collect.Lists;
//import org.artifactory.rest.common.model.artifactorylicense.LicensesDetails;
//import org.codehaus.jackson.map.ObjectMapper;
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.io.IOException;
//import java.util.List;
//
///**
// * @author Shay Bagants
// */
//@Test
//public class LicensesModelTest {
//
//    private final String LICENSES_JSON = "{\n" +
//            "  \"licenses\" : [ {\n" +
//            "    \"licenseHash\" : \"jb12tcBe1seX6yg\",\n" +
//            "    \"licensedTo\" : \"JFrog inc.\",\n" +
//            "    \"validThrough\" : \"21-06-2018\",\n" +
//            "    \"type\" : \"Enterprise\",\n" +
//            "    \"nodeId\" : \"art_03\",\n" +
//            "    \"nodeUrl\" : \"http://172.17.0.19:8081\",\n" +
//            "    \"expired\" : false\n" +
//            "  }, {\n" +
//            "    \"licenseHash\" : \"jd19jf19jf11j9\",\n" +
//            "    \"licensedTo\" : \"JFrog ltd.\",\n" +
//            "    \"validThrough\" : \"21-06-2019\",\n" +
//            "    \"type\" : \"Enterprise\",\n" +
//            "    \"nodeId\" : \"art_03\",\n" +
//            "    \"nodeUrl\" : \"http://172.17.0.19:8082\",\n" +
//            "    \"expired\" : false\n" +
//            "  } ]\n" +
//            "}";
//
//    public void objectToString() throws IOException {
//        LicensesDetails licensesModel = new LicensesDetails();
//        List<LicensesDetails.LicenseModel> allLicenses = Lists.newArrayList();
//
//        // License 1
//        LicensesDetails.LicenseModel license1 = new LicensesDetails.LicenseModel();
//        license1.setLicenseHash("jb12tcBe1seX6yg");
//        license1.setLicensedTo("JFrog inc.");
//        license1.setExpired(false);
//        license1.setValidThrough("21-06-2018");
//        license1.setType("Enterprise");
//        license1.setNodeId("art_03");
//        license1.setNodeUrl("http://172.17.0.19:8081");
//        allLicenses.add(license1);
//
//        // License 2
//        LicensesDetails.LicenseModel license2 = new LicensesDetails.LicenseModel();
//        license2.setLicenseHash("jd19jf19jf11j9");
//        license2.setLicensedTo("JFrog ltd.");
//        license2.setExpired(false);
//        license2.setValidThrough("21-06-2019");
//        license2.setType("Enterprise");
//        license2.setNodeId("art_03");
//        license2.setNodeUrl("http://172.17.0.19:8082");
//        allLicenses.add(license2);
//
//        licensesModel.setLicenses(allLicenses);
//
//        // Write object to String and ensure that it's equals to 'LICENSES_JSON'
//        ObjectMapper mapper = new ObjectMapper();
//        String licensesString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(licensesModel);
//        Assert.assertEquals(licensesString, LICENSES_JSON);
//    }
//}
