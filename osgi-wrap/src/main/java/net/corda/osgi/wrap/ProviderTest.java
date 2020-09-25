package net.corda.osgi.wrap;

import java.security.Security;

public class ProviderTest {

    public static void main(String[] args) {

        String providerName = "BC";

        if (Security.getProvider(providerName) == null) {
            System.out.println(providerName + " provider not installed!");
        } else {
            System.out.println(providerName + " is installed.");
        }

    }

}
