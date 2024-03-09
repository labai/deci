package com.github.labai.deci.converter.jpa2;

/**
 * @author Augustus
 * created on 2020.11.27
 *
 * <p>
 * can be used to for package scanning configuration
 *
 * <p>
 * factory.setPackagesToScan("your.jpa.package", Jpa2DeciRegister.PACKAGE);
 *
 */
public final class Jpa2DeciRegister {
    public static final String PACKAGE = Jpa2DeciRegister.class.getPackage().getName();
}
