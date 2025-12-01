/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

/**
 *
 * @author HP
 */
import de.schlichtherle.license.CipherParam;
import de.schlichtherle.license.KeyStoreParam;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import de.schlichtherle.license.LicenseParam;
import java.io.File;
import java.util.Date;
import java.util.prefs.Preferences;
import javax.security.auth.x500.X500Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;


public class LicenseServer {

    // TODO move this to the properties file
    @Autowired
    private Environment env;

    private static final String APP_VERSION = "1.1";
    private static final String PROPERTIES_FILENAME = "DSLicenseServer.properties";

    // get these from properties file
    private String appName;
    private String dataFileExtension;
    private String licenseFileExtension;

    // keystore information (from properties file)
    private static String keystoreFilename;     // this app needs the "private" keystore
    private static String keystorePassword;
    private static String keyPassword;
    private static String alias;
    private static String cipherParamPassword;  // 6+ chars, and both letters and numbers
    // built by our app
    KeyStoreParam privateKeyStoreParam ;
    private CipherParam cipherParam;

    // exit status codes
    private static int EXIT_STATUS_ALL_GOOD = 0;
    private static int EXIT_STATUS_ERR_WRONG_NUM_ARGS = 1;
    private static int EXIT_STATUS_ERR_EXCEPTION_THROWN = 2;
    private static int EXIT_STATUS_ERR_CANT_READ_DATA_FILE = 3;
    private static int EXIT_STATUS_ERR_CANT_OUR_PROPERTIES_FILE = 4;

    // properties we get from the data file, and write to the license file
    private String firstName;
    private String lastName;
    private String city;
    private String state;
    private String country;

//    public static void main(String[] args) {
//
//        // should have one arg, and it should be the basename of the file(s)
//        if (args.length != 2) {
//            System.err.println("Need two args: [directory] [baseFilename]");
//            System.exit(EXIT_STATUS_ERR_WRONG_NUM_ARGS);
//        }
//
//        // args ok, run program
//        new LicenseServer("resources", "resources");
//    }
    public LicenseManager LicenseKey(final String directory, final String fileBasename) {
        // load all the properties we need to run
        loadOurPropertiesFile();

        // read all the attributes from the data file for this customer
        loadInfoFromCustomerDataFile(directory, fileBasename);

        // set up an implementation of the KeyStoreParam interface that returns 
        // the information required to work with the keystore containing the private key:
        privateKeyStoreParam = new LicenceServerImp();

        // Set up an implementation of the CipherParam interface to return the password to be
        // used when performing the PKCS-5 encryption.
        cipherParam = () -> cipherParamPassword;

        // Set up an implementation of the LicenseParam interface.
        // Note that the subject string returned by getSubject() must match the subject property
        // of any LicenseContent instance to be used with this LicenseParam instance.
        LicenseParam licenseParam = new LicenseParam() {
            @Override
            public String getSubject() {
                return appName;
            }

            @Override
            public Preferences getPreferences() {
                // TODO why is this needed for the app that creates the license?
                //return Preferences.userNodeForPackage(LicenseServer.class);
                return null;
            }

            @Override
            public KeyStoreParam getKeyStoreParam() {
                return privateKeyStoreParam;
            }

            @Override
            public CipherParam getCipherParam() {
                return cipherParam;
            }
        };

        // create the license file
        LicenseManager lm = new LicenseManager(licenseParam);
        try {
            // write the file to the same directory we read it in from
            String filename = directory + "/" + fileBasename + licenseFileExtension;
            lm.store(createLicenseContent(licenseParam), new File(filename));
            System.exit(EXIT_STATUS_ALL_GOOD);
        } catch (Exception e) {
            System.exit(EXIT_STATUS_ERR_EXCEPTION_THROWN);
        }
        return lm;
    }

    /**
     * Load the general properties this application needs in order to run.
     */
    private void loadOurPropertiesFile() {
        appName = env.getProperty("spring.cilantro.app_name");
        System.out.println("APP NAME: " + appName);
        dataFileExtension = env.getProperty("spring.cilantro.data_file_extension");
        licenseFileExtension = env.getProperty("spring.cilantro.license_file_extension");
        keystoreFilename = env.getProperty("spring.cilantro.keystore_filename");
        keystorePassword = env.getProperty("spring.cilantro.keystore_password");
        alias = env.getProperty("spring.cilantro.alias");
        keyPassword = env.getProperty("spring.cilantro.key_password");
        cipherParamPassword = env.getProperty("spring.cilantro.cipher_param_password");
    }

    /**
     * Read the data file that has information about the current customer.
     *
     * @param directory The directory where the properties file is located.
     * @param fileBasename The base portion of the filename.
     */
    private void loadInfoFromCustomerDataFile(final String directory, final String fileBasename) {
//        Properties properties = new Properties();
//        FileInputStream in;
        try {
//            in = new FileInputStream(directory + "/" + fileBasename + dataFileExtension);
//            properties.load(in);
//            in.close();
            firstName = "Melleji";//properties.getProperty("first_name");
            lastName = "Mollel";//properties.getProperty("last_name");
            city = "Dar-es-salaam";//properties.getProperty("city");
            state = "Dar-es-salaam";//properties.getProperty("state");
            country = "TZ";//properties.getProperty("country");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(EXIT_STATUS_ERR_CANT_READ_DATA_FILE);
        }
    }

    // Set up the LicenseContent instance. This is the information that will be in the
    // generated license file.
    private LicenseContent createLicenseContent(LicenseParam licenseParam) {
        LicenseContent result = new LicenseContent();
        X500Principal holder = new X500Principal("CN=" + firstName + " " + lastName + ", "
                + "L=" + city + ", "
                + "ST=" + state + ", "
                + "C=" + country);
        result.setHolder(holder);
        X500Principal issuer = new X500Principal(
                "CN=devdaily.com, L=Simpsonville, ST=KY, "
                + " OU=Software Development,"
                + " O=DevDaily Interactive,"
                + " C=United States,"
                + " DC=US");
        result.setIssuer(issuer);
        // i'm selling one license
        result.setConsumerAmount(1);
        // i think this needs to be "user"
        result.setConsumerType("User");
        result.setInfo("License key for the " + appName + " application.");
        Date now = new Date();
        result.setIssued(now);
        now.setYear(now.getYear() + 1);
        result.setNotAfter(now);
        result.setSubject(licenseParam.getSubject());
        return result;
    }

}
