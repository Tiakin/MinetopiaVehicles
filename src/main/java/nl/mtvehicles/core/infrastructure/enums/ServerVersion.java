package nl.mtvehicles.core.infrastructure.enums;

import nl.mtvehicles.core.infrastructure.annotations.VersionSpecific;
import org.jetbrains.annotations.NotNull;

/**
 * Enum of supported server versions (used for different NMS and Spigot API changes)
 */
@VersionSpecific
public enum ServerVersion {
    /**
     * 1.12-1.12.2
     */
    v1_12_R1,
    /**
     * 1.13.1-1.13.2
     */
    v1_13_R2,
    /**
     * 1.15-1.15.2
     */
    v1_15_R1,
    /**
     * 1.16.4-1.16.5
     */
    v1_16_R3,
    /**
     * 1.17-1.17.1
     */
    v1_17_R1,
    /**
     * 1.18-1.18.1
     */
    v1_18_R1,
    /**
     * 1.18.2
     */
    v1_18_R2,
    /**
     * 1.19-1.19.2
     * @since 2.4.3
     */
    v1_19_R1,
    /**
     * 1.19.3
     * @since 2.5.0
     */
    v1_19_R2,
    /**
     * 1.19.4
     * @since 2.5.0
     */
    v1_19_R3,
    /**
     * 1.20 and 1.20.1
     * @since 2.5.6
     */
    v1_20_R1,
    /**
     * 1.20.2
     * @since 2.5.2
     */
    v1_20_R2,
    /**
     * 1.20.3 and 1.20.4
     * @since 2.5.2
     */
    v1_20_R3,
    /**
     * 1.20.5 and 1.20.6
     * @since 2.5.4
     */
    v1_20_R4,
    /**
     * 1.21 and 1.21.1
     * @since 2.5.4
     */
    v1_21_R1,
    /**
     * 1.21.2 and 1.21.3
     * @since 2.5.5
     */
    v1_21_R2,
    /**
     * 1.21.4
     * @since 2.5.5
     */
    v1_21_R3,
    /**
     * 1.21.5
     * @since 2.5.6
     */
    v1_21_R4,
    /**
     * 1.21.6, 1.21.7 and 1.21.8
     * 
     * @since 2.5.6
     */
    v1_21_R5;

    public boolean is1_12_R1(){return this.equals(v1_12_R1);}

    public boolean is1_13_R2(){return this.equals(v1_13_R2);}

    public boolean is1_15_R1(){return this.equals(v1_15_R1);}

    public boolean is1_16_R3(){return this.equals(v1_16_R3);}

    public boolean is1_17_R1(){return this.equals(v1_17_R1);}

    public boolean is1_18_R1(){return this.equals(v1_18_R1);}

    public boolean is1_18_R2(){return this.equals(v1_18_R2);}

    public boolean is1_19_R1(){return this.equals(v1_19_R1);}

    public boolean is1_19_R2(){return this.equals(v1_19_R2);}

    public boolean is1_19_R3(){return this.equals(v1_19_R3);}

    public boolean is1_20_R1() {return this.equals(v1_20_R1);}

    public boolean is1_20_R2() {return this.equals(v1_20_R2);}

    public boolean is1_20_R3() {return this.equals(v1_20_R3);}

    public boolean is1_20_R4(){return this.equals(v1_20_R4);}

    public boolean is1_21_R1(){return this.equals(v1_21_R1);}

    public boolean is1_21_R2(){return this.equals(v1_21_R2);}

    public boolean is1_21_R3(){return this.equals(v1_21_R3);}

    public boolean is1_21_R4(){return this.equals(v1_21_R4);}
    
    public boolean is1_21_R5(){return this.equals(v1_21_R5);}

    /**
     * Check whether the server version is older than the given one
     */
    public boolean isOlderThan(@NotNull ServerVersion version){
        return this.ordinal() < version.ordinal();
    }

    /**
     * Check whether the server version is older than the given one or whether it is the same
     */
    public boolean isOlderOrEqualTo(@NotNull ServerVersion version){
        return this.ordinal() <= version.ordinal();
    }

    /**
     * Check whether the server version is newer than the given one
     */
    public boolean isNewerThan(@NotNull ServerVersion version){
        return this.ordinal() > version.ordinal();
    }

    /**
     * Check whether the server version is newer than the given one or whether it is the same
     */
    public boolean isNewerOrEqualTo(@NotNull ServerVersion version){
        return this.ordinal() >= version.ordinal();
    }

}
