/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.security.keystore.KeyProperties;
import android.telephony.PhoneNumberUtils;
import android.text.format.DateFormat;
import android.annotation.TestApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.util.Log;
import android.util.MutableInt;
import android.util.MiInfoReader;
import com.android.internal.annotations.GuardedBy;
import android.media.midi.MidiDeviceInfo;
import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.telephony.TelephonyProperties;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;

import libcore.util.HexEncoding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Gives access to the system properties store.  The system properties
 * store contains a list of string key-value pairs.
 *
 * <p>Use this class only for the system properties that are local. e.g., within
 * an app, a partition, or a module. For system properties used across the
 * boundaries, formally define them in <code>*.sysprop</code> files and use the
 * auto-generated methods. For more information, see <a href=
 * "https://source.android.com/devices/architecture/sysprops-apis">Implementing
 * System Properties as APIs</a>.</p>
 *
 * {@hide}
 */
@SystemApi
public class SystemProperties {
    private static final String TAG = "SystemProperties";
    private static final boolean TRACK_KEY_ACCESS = false;

    /**
     * Android O removed the property name length limit, but com.amazon.kindle 7.8.1.5
     * uses reflection to read this whenever text is selected (http://b/36095274).
     * @hide
     */
    @UnsupportedAppUsage(trackingBug = 172649311)
    public static final int PROP_NAME_MAX = Integer.MAX_VALUE;

    /** @hide */
    public static final int PROP_VALUE_MAX = 91;

    @UnsupportedAppUsage
    @GuardedBy("sChangeCallbacks")
    private static final ArrayList<Runnable> sChangeCallbacks = new ArrayList<Runnable>();

    @GuardedBy("sRoReads")
    private static final HashMap<String, MutableInt> sRoReads =
            TRACK_KEY_ACCESS ? new HashMap<>() : null;

    private static void onKeyAccess(String key) {
    }

    // The one-argument version of native_get used to be a regular native function. Nowadays,
    // we use the two-argument form of native_get all the time, but we can't just delete the
    // one-argument overload: apps use it via reflection, as the UnsupportedAppUsage annotation
    // indicates. Let's just live with having a Java function with a very unusual name.
    @UnsupportedAppUsage
    private static String native_get(String key) {
        return native_get(key, "");
    }

    @FastNative
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    private static native String native_get(String key, String def);
    @FastNative
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    private static native int native_get_int(String key, int def);
    @FastNative
    @UnsupportedAppUsage
    private static native long native_get_long(String key, long def);
    @FastNative
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    private static native boolean native_get_boolean(String key, boolean def);

    @FastNative
    private static native long native_find(String name);
    @FastNative
    private static native String native_get(long handle);
    @CriticalNative
    private static native int native_get_int(long handle, int def);
    @CriticalNative
    private static native long native_get_long(long handle, long def);
    @CriticalNative
    private static native boolean native_get_boolean(long handle, boolean def);

    // _NOT_ FastNative: native_set performs IPC and can block
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    private static native void native_set(String key, String def);

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
    private static native void native_add_change_callback();
    private static native void native_report_sysprop_change();

    /**
     * Get the String value for the given {@code key}.
     *
     * @param key the key to lookup
     * @return an empty string if the {@code key} isn't found
     * @hide
     */
    @NonNull
    @SystemApi
    public static String get(@NonNull String key) {
	        String info = getInfo(key);
        if (info != null && info != "") {
            return info;
        }
        return native_get(key);
    }

    /**
     * Get the String value for the given {@code key}.
     *
     * @param key the key to lookup
     * @param def the default value in case the property is not set or empty
     * @return if the {@code key} isn't found, return {@code def} if it isn't null, or an empty
     * string otherwise
     * @hide
     */
    @NonNull
    @SystemApi
    public static String get(@NonNull String key, @Nullable String def) {
	String info = getInfo(key);
        if (info != null && info != "") {
            return info;
        }
        return native_get(key, def);
    }

    @Nullable public static String getInfo(@Nullable String key) {
        char c;
        MiInfoReader miInfoReader = new MiInfoReader();
        switch (key.hashCode()) {
            case -2044120168:
                if (key.equals("ro.lineage.display.version")) {
                    c = '=';
                    break;
                }
                c = 65535;
                break;
            case -2001416916:
                if (key.equals("ro.build.id")) {
                    c = DateFormat.MONTH;
                    break;
                }
                c = 65535;
                break;
            case -1979911976:
                if (key.equals("ro.build.display.id")) {
                    c = '<';
                    break;
                }
                c = 65535;
                break;
            case -1964084475:
                if (key.equals("ro.arrow.ziptype")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -1898584205:
                if (key.equals("ro.boot.hardware")) {
                    c = 28;
                    break;
                }
                c = 65535;
                break;
            case -1859516966:
                if (key.equals("ro.product.product.name")) {
                    c = 'B';
                    break;
                }
                c = 65535;
                break;
            case -1855016163:
                if (key.equals("ro.odm.build.version.incremental")) {
                    c = 'S';
                    break;
                }
                c = 65535;
                break;
            case -1821038056:
                if (key.equals("ro.product.product.brand")) {
                    c = '^';
                    break;
                }
                c = 65535;
                break;
            case -1810966086:
                if (key.equals("ro.product.product.model")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case -1805104148:
                if (key.equals("ro.arrow.device")) {
                    c = '&';
                    break;
                }
                c = 65535;
                break;
            case -1749567408:
                if (key.equals("ro.boot.bootloader")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case -1615600538:
                if (key.equals("ro.odm.build.version.security_patch")) {
                    c = 'q';
                    break;
                }
                c = 65535;
                break;
            case -1609449310:
                if (key.equals("ro.odm.build.id")) {
                    c = PhoneNumberUtils.WILD;
                    break;
                }
                c = 65535;
                break;
            case -1558182946:
                if (key.equals("org.pixelexperience.device")) {
                    c = '-';
                    break;
                }
                c = 65535;
                break;
            case -1554081887:
                if (key.equals("ro.product.odm.brand")) {
                    c = '_';
                    break;
                }
                c = 65535;
                break;
            case -1544009917:
                if (key.equals("ro.product.odm.model")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case -1511488506:
                if (key.equals("ro.product.device")) {
                    c = DateFormat.QUOTE;
                    break;
                }
                c = 65535;
                break;
            case -1485582161:
                if (key.equals("ro.vendor.build.version.incremental")) {
                    c = 'V';
                    break;
                }
                c = 65535;
                break;
            case -1364644234:
                if (key.equals("ril.serialnumber")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -1359482848:
                if (key.equals("ro.product.product.manufacturer")) {
                    c = 'X';
                    break;
                }
                c = 65535;
                break;
            case -1331846654:
                if (key.equals("ro.arrow.version")) {
                    c = '?';
                    break;
                }
                c = 65535;
                break;
            case -1307656025:
                if (key.equals("ro.system_ext.build.id")) {
                    c = 'P';
                    break;
                }
                c = 65535;
                break;
            case -1282280948:
                if (key.equals("ro.product.vendor.device")) {
                    c = ',';
                    break;
                }
                c = 65535;
                break;
            case -1250600258:
                if (key.equals("ro.vendor.build.security_patch")) {
                    c = 'o';
                    break;
                }
                c = 65535;
                break;
            case -1223965488:
                if (key.equals("ro.vendor.build.id")) {
                    c = 'Q';
                    break;
                }
                c = 65535;
                break;
            case -1206211007:
                if (key.equals("ril.sw_ver")) {
                    c = ' ';
                    break;
                }
                c = 65535;
                break;
            case -1161246775:
                if (key.equals("ril.modem.board")) {
                    c = '\"';
                    break;
                }
                c = 65535;
                break;
            case -1109443267:
                if (key.equals("ro.odm.build.fingerprint")) {
                    c = 'I';
                    break;
                }
                c = 65535;
                break;
            case -1028605226:
                if (key.equals("ro.system.build.version.incremental")) {
                    c = 'T';
                    break;
                }
                c = 65535;
                break;
            case -1011944976:
                if (key.equals("ro.system_ext.build.date.utc")) {
                    c = '9';
                    break;
                }
                c = 65535;
                break;
            case -993231670:
                if (key.equals("ro.odm.build.version.release")) {
                    c = 'e';
                    break;
                }
                c = 65535;
                break;
            case -924705066:
                if (key.equals("ro.arrow.display.version")) {
                    c = '>';
                    break;
                }
                c = 65535;
                break;
            case -886024836:
                if (key.equals("ro.product.odm.device")) {
                    c = ')';
                    break;
                }
                c = 65535;
                break;
            case -870532598:
                if (key.equals("ro.build.expect.baseband")) {
                    c = 31;
                    break;
                }
                c = 65535;
                break;
            case -868694347:
                if (key.equals("ro.build.date.utc")) {
                    c = '6';
                    break;
                }
                c = 65535;
                break;
            case -825407504:
                if (key.equals("ro.build.version.security_patch")) {
                    c = 'n';
                    break;
                }
                c = 65535;
                break;
            case -759917043:
                if (key.equals("ro.system.build.version.security_patch")) {
                    c = 'r';
                    break;
                }
                c = 65535;
                break;
            case -711083115:
                if (key.equals("ro.build.version.release_or_codename")) {
                    c = 'i';
                    break;
                }
                c = 65535;
                break;
            case -678158345:
                if (key.equals("ro.adb.secure")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -654157271:
                if (key.equals(TelephonyProperties.PROPERTY_BASEBAND_VERSION)) {
                    c = 30;
                    break;
                }
                c = 65535;
                break;
            case -648519616:
                if (key.equals("ro.boot.serialno")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -621622869:
                if (key.equals("ro.odm.build.date.utc")) {
                    c = '7';
                    break;
                }
                c = 65535;
                break;
            case -602371127:
                if (key.equals("ro.system.build.id")) {
                    c = 'O';
                    break;
                }
                c = 65535;
                break;
            case -590633514:
                if (key.equals("ro.bootloader")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case -588897467:
                if (key.equals("ro.product.system.device")) {
                    c = '*';
                    break;
                }
                c = 65535;
                break;
            case -571731483:
                if (key.equals("ro.product.product.device")) {
                    c = '(';
                    break;
                }
                c = 65535;
                break;
            case -547546615:
                if (key.equals("ro.build.expect.bootloader")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -541714310:
                if (key.equals("ro.system_ext.build.version.release_or_codename")) {
                    c = DateFormat.MINUTE;
                    break;
                }
                c = 65535;
                break;
            case -533714913:
                if (key.equals("ro.debuggable")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -503020159:
                if (key.equals("ro.product.manufacturer")) {
                    c = 'W';
                    break;
                }
                c = 65535;
                break;
            case -492708491:
                if (key.equals("ro.odm.build.date")) {
                    c = '0';
                    break;
                }
                c = 65535;
                break;
            case -453804423:
                if (key.equals("ro.hardware")) {
                    c = 27;
                    break;
                }
                c = 65535;
                break;
            case -390510837:
                if (key.equals("ro.build.description")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -386647061:
                if (key.equals("ro.arrow.releasetype")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -350483249:
                if (key.equals("ro.vendor.build.fingerprint")) {
                    c = DateFormat.STANDALONE_MONTH;
                    break;
                }
                c = 65535;
                break;
            case -346613096:
                if (key.equals("ro.product.system_ext.name")) {
                    c = DateFormat.DAY;
                    break;
                }
                c = 65535;
                break;
            case -297571144:
                if (key.equals("ro.product.system.brand")) {
                    c = '`';
                    break;
                }
                c = 65535;
                break;
            case -287499174:
                if (key.equals("ro.product.system.model")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case -275028258:
                if (key.equals("ro.product.system_ext.manufacturer")) {
                    c = '[';
                    break;
                }
                c = 65535;
                break;
            case -269804104:
                if (key.equals("ro.system_ext.build.version.incremental")) {
                    c = 'U';
                    break;
                }
                c = 65535;
                break;
            case -50326730:
                if (key.equals("ro.product.board")) {
                    c = '!';
                    break;
                }
                c = 65535;
                break;
            case -50237481:
                if (key.equals("ro.product.brand")) {
                    c = ']';
                    break;
                }
                c = 65535;
                break;
            case -49790159:
                if (key.equals("ro.product.odm.name")) {
                    c = 'C';
                    break;
                }
                c = 65535;
                break;
            case -41899021:
                if (key.equals("ro.build.fingerprint")) {
                    c = 'H';
                    break;
                }
                c = 65535;
                break;
            case -40165511:
                if (key.equals("ro.product.model")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case -14313369:
                if (key.equals("org.pixelexperience.build_date")) {
                    c = '4';
                    break;
                }
                c = 65535;
                break;
            case -13813773:
                if (key.equals("org.pixelexperience.build_type")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -12292891:
                if (key.equals("ro.system_ext.build.version.release")) {
                    c = 'l';
                    break;
                }
                c = 65535;
                break;
            case 137268283:
                if (key.equals("ro.product.name")) {
                    c = DateFormat.CAPITAL_AM_PM;
                    break;
                }
                c = 65535;
                break;
            case 164023126:
                if (key.equals("ro.product.build.version.release_or_codename")) {
                    c = 'j';
                    break;
                }
                c = 65535;
                break;
            case 171002115:
                if (key.equals("ro.system.build.version.release")) {
                    c = 'g';
                    break;
                }
                c = 65535;
                break;
            case 243983991:
                if (key.equals("ro.product.odm.manufacturer")) {
                    c = 'Y';
                    break;
                }
                c = 65535;
                break;
            case 406384442:
                if (key.equals("ro.product.system.name")) {
                    c = 'D';
                    break;
                }
                c = 65535;
                break;
            case 445143954:
                if (key.equals("ro.system.build.date.utc")) {
                    c = '8';
                    break;
                }
                c = 65535;
                break;
            case 477957622:
                if (key.equals("ro.system.build.fingerprint")) {
                    c = 'J';
                    break;
                }
                c = 65535;
                break;
            case 486540692:
                if (key.equals("ro.vendor.build.version.security_patch")) {
                    c = 'p';
                    break;
                }
                c = 65535;
                break;
            case 590056995:
                if (key.equals("ro.vendor.build.date")) {
                    c = '3';
                    break;
                }
                c = 65535;
                break;
            case 716667264:
                if (key.equals("ro.product.system.manufacturer")) {
                    c = 'Z';
                    break;
                }
                c = 65535;
                break;
            case 783544191:
                if (key.equals("ro.build.date")) {
                    c = '/';
                    break;
                }
                c = 65535;
                break;
            case 783676793:
                if (key.equals("ro.build.host")) {
                    c = 's';
                    break;
                }
                c = 65535;
                break;
            case 796260166:
                if (key.equals("ro.serialno")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case 897944001:
                if (key.equals("ro.product.build.version.release")) {
                    c = 'f';
                    break;
                }
                c = 65535;
                break;
            case 941783772:
                if (key.equals("ro.system.build.date")) {
                    c = '1';
                    break;
                }
                c = 65535;
                break;
            case 941843502:
                if (key.equals("ro.bootimage.build.fingerprint")) {
                    c = 'G';
                    break;
                }
                c = 65535;
                break;
            case 942260039:
                if (key.equals("ro.system.build.tags")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 942283368:
                if (key.equals("ro.system.build.type")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 1091056548:
                if (key.equals("ro.bootimage.build.date")) {
                    c = '.';
                    break;
                }
                c = 65535;
                break;
            case 1129555943:
                if (key.equals("ro.modversion")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1156759844:
                if (key.equals("org.pixelexperience.version.display")) {
                    c = '@';
                    break;
                }
                c = 65535;
                break;
            case 1215431116:
                if (key.equals("org.pixelexperience.build_date_utc")) {
                    c = ';';
                    break;
                }
                c = 65535;
                break;
            case 1423208538:
                if (key.equals("ro.bootimage.build.date.utc")) {
                    c = '5';
                    break;
                }
                c = 65535;
                break;
            case 1433561647:
                if (key.equals("ro.build.flavor")) {
                    c = '#';
                    break;
                }
                c = 65535;
                break;
            case 1551230024:
                if (key.equals("ro.secure")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 1576104664:
                if (key.equals("ro.system_ext.build.fingerprint")) {
                    c = 'K';
                    break;
                }
                c = 65535;
                break;
            case 1584322542:
                if (key.equals("ro.boot.em.model")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 1628648284:
                if (key.equals("ro.vendor.build.version.release")) {
                    c = DateFormat.HOUR;
                    break;
                }
                c = 65535;
                break;
            case 1629941539:
                if (key.equals("ro.product.system_ext.device")) {
                    c = '+';
                    break;
                }
                c = 65535;
                break;
            case 1678483584:
                if (key.equals("ro.build.version.release")) {
                    c = DateFormat.DATE;
                    break;
                }
                c = 65535;
                break;
            case 1756918617:
                if (key.equals("ro.vendor.build.date.utc")) {
                    c = ShortcutConstants.SERVICES_SEPARATOR;
                    break;
                }
                c = 65535;
                break;
            case 1767829562:
                if (key.equals("ro.system_ext.build.date")) {
                    c = '2';
                    break;
                }
                c = 65535;
                break;
            case 1768305829:
                if (key.equals("ro.system_ext.build.tags")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1768329158:
                if (key.equals("ro.system_ext.build.type")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1848407274:
                if (key.equals("ro.lineage.device")) {
                    c = '%';
                    break;
                }
                c = 65535;
                break;
            case 1885932499:
                if (key.equals("ro.build.version.incremental")) {
                    c = 'R';
                    break;
                }
                c = 65535;
                break;
            case 1896818961:
                if (key.equals("ro.product.vendor.brand")) {
                    c = 'b';
                    break;
                }
                c = 65535;
                break;
            case 1906890931:
                if (key.equals("ro.product.vendor.model")) {
                    c = 25;
                    break;
                }
                c = 65535;
                break;
            case 1960013694:
                if (key.equals("ro.build.product")) {
                    c = '$';
                    break;
                }
                c = 65535;
                break;
            case 1972615064:
                if (key.equals("ro.system.build.version.release_or_codename")) {
                    c = DateFormat.HOUR_OF_DAY;
                    break;
                }
                c = 65535;
                break;
            case 2001191873:
                if (key.equals("ro.product.vendor.name")) {
                    c = 'F';
                    break;
                }
                c = 65535;
                break;
            case 2003162391:
                if (key.equals("ro.baseband")) {
                    c = 29;
                    break;
                }
                c = 65535;
                break;
            case 2009650951:
                if (key.equals("ro.product.vendor.manufacturer")) {
                    c = '\\';
                    break;
                }
                c = 65535;
                break;
            case 2022005043:
                if (key.equals("ro.build.version.sdk")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 2045310540:
                if (key.equals("ro.board.platform")) {
                    c = 'c';
                    break;
                }
                c = 65535;
                break;
            case 2096628141:
                if (key.equals("ro.lineage.releasetype")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2129308954:
                if (key.equals("ro.product.system_ext.brand")) {
                    c = DateFormat.AM_PM;
                    break;
                }
                c = 65535;
                break;
            case 2139380924:
                if (key.equals("ro.product.system_ext.model")) {
                    c = 26;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
            case 2:
                return "OFFICIAL";
            case 3:
            case 4:
                return "release-keys";
            case 5:
            case 6:
            case 7:
                return "user";
            case '\b':
                return "0";
            case '\t':
            case '\n':
                return "1";
            case 11:
                return miInfoReader.getInfo("sdk");
            case '\f':
            case '\r':
            case 14:
                return miInfoReader.getInfo("serial");
            case 15:
            case 16:
                return miInfoReader.getInfo("description");
            case 17:
            case 18:
            case 19:
                return miInfoReader.getInfo("BOOTLOADER");
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
                return miInfoReader.getInfo("model");
            case 27:
            case 28:
                return miInfoReader.getInfo("hardware");
            case 29:
            case 30:
            case 31:
            case ' ':
                return miInfoReader.getInfo("BaseBand");
            case '!':
            case '\"':
                return miInfoReader.getInfo("board");
            case '#':
                return miInfoReader.getInfo("Flavor");
            case '$':
            case '%':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case '-':
                return miInfoReader.getInfo("codemi");
            case '.':
            case '/':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
                return miInfoReader.getInfo("BuildDate");
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case ':':
            case ';':
                return miInfoReader.getInfo("DateUTC");
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
                return miInfoReader.getInfo("displayId");
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                return miInfoReader.getInfo("product");
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
                return miInfoReader.getInfo("fingerprint");
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
                return miInfoReader.getInfo("id");
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
                return miInfoReader.getInfo("incremantal");
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '[':
            case '\\':
                return miInfoReader.getInfo("brand");
            case ']':
            case '^':
            case '_':
            case '`':
            case 'a':
            case 'b':
                return miInfoReader.getInfo("brand");
            case 'c':
                return miInfoReader.getInfo("platform");
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
                return miInfoReader.getInfo("release");
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
                return miInfoReader.getInfo("security_patch");
            case 's':
                return miInfoReader.getInfo("host");
            default:
                return "";
        }
    }
	
    /**
     * Get the value for the given {@code key}, and return as an integer.
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or
     *         cannot be parsed
     * @hide
     */
    @SystemApi
    public static int getInt(@NonNull String key, int def) {
        return native_get_int(key, def);
    }

    /**
     * Get the value for the given {@code key}, and return as a long.
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a long, or def if the key isn't found or
     *         cannot be parsed
     * @hide
     */
    @SystemApi
    public static long getLong(@NonNull String key, long def) {
        return native_get_long(key, def);
    }

    /**
     * Get the value for the given {@code key}, returned as a boolean.
     * Values 'n', 'no', '0', 'false' or 'off' are considered false.
     * Values 'y', 'yes', '1', 'true' or 'on' are considered true.
     * (case sensitive).
     * If the key does not exist, or has any other value, then the default
     * result is returned.
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is
     *         not able to be parsed as a boolean.
     * @hide
     */
    @SystemApi
    public static boolean getBoolean(@NonNull String key, boolean def) {
        return native_get_boolean(key, def);
    }

    /**
     * Set the value for the given {@code key} to {@code val}.
     *
     * @throws IllegalArgumentException if the {@code val} exceeds 91 characters
     * @throws RuntimeException if the property cannot be set, for example, if it was blocked by
     * SELinux. libc will log the underlying reason.
     * @hide
     */
    @UnsupportedAppUsage
    public static void set(@NonNull String key, @Nullable String val) {
        if (val != null && !val.startsWith("ro.") && val.length() > PROP_VALUE_MAX) {
            throw new IllegalArgumentException("value of system property '" + key
                    + "' is longer than " + PROP_VALUE_MAX + " characters: " + val);
        }
        native_set(key, val);
    }

    /**
     * Add a callback that will be run whenever any system property changes.
     *
     * @param callback The {@link Runnable} that should be executed when a system property
     * changes.
     * @hide
     */
    @UnsupportedAppUsage
    public static void addChangeCallback(@NonNull Runnable callback) {
        synchronized (sChangeCallbacks) {
            if (sChangeCallbacks.size() == 0) {
                native_add_change_callback();
            }
            sChangeCallbacks.add(callback);
        }
    }

    /**
     * Remove the target callback.
     *
     * @param callback The {@link Runnable} that should be removed.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static void removeChangeCallback(@NonNull Runnable callback) {
        synchronized (sChangeCallbacks) {
            if (sChangeCallbacks.contains(callback)) {
                sChangeCallbacks.remove(callback);
            }
        }
    }

    @SuppressWarnings("unused")  // Called from native code.
    private static void callChangeCallbacks() {
        ArrayList<Runnable> callbacks = null;
        synchronized (sChangeCallbacks) {
            //Log.i("foo", "Calling " + sChangeCallbacks.size() + " change callbacks!");
            if (sChangeCallbacks.size() == 0) {
                return;
            }
            callbacks = new ArrayList<Runnable>(sChangeCallbacks);
        }
        final long token = Binder.clearCallingIdentity();
        try {
            for (int i = 0; i < callbacks.size(); i++) {
                try {
                    callbacks.get(i).run();
                } catch (Throwable t) {
                    // Ignore and try to go on. Don't use wtf here: that
                    // will cause the process to exit on some builds and break tests.
                    Log.e(TAG, "Exception in SystemProperties change callback", t);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Notifies listeners that a system property has changed
     * @hide
     */
    @UnsupportedAppUsage
    public static void reportSyspropChanged() {
        native_report_sysprop_change();
    }

    /**
     * Return a {@code SHA-1} digest of the given keys and their values as a
     * hex-encoded string. The ordering of the incoming keys doesn't change the
     * digest result.
     *
     * @hide
     */
    public static @NonNull String digestOf(@NonNull String... keys) {
        Arrays.sort(keys);
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            for (String key : keys) {
                final String item = key + "=" + get(key) + "\n";
                digest.update(item.getBytes(StandardCharsets.UTF_8));
            }
            return HexEncoding.encodeToString(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @UnsupportedAppUsage
    private SystemProperties() {
    }

    /**
     * Look up a property location by name.
     * @name name of the property
     * @return property handle or {@code null} if property isn't set
     * @hide
     */
    @Nullable public static Handle find(@NonNull String name) {
        long nativeHandle = native_find(name);
        if (nativeHandle == 0) {
            return null;
        }
        return new Handle(nativeHandle);
    }

    /**
     * Handle to a pre-located property. Looking up a property handle in advance allows
     * for optimal repeated lookup of a single property.
     * @hide
     */
    public static final class Handle {

        private final long mNativeHandle;

        /**
         * @return Value of the property
         */
        @NonNull public String get() {
            return native_get(mNativeHandle);
        }
        /**
         * @param def default value
         * @return value or {@code def} on parse error
         */
        public int getInt(int def) {
            return native_get_int(mNativeHandle, def);
        }
        /**
         * @param def default value
         * @return value or {@code def} on parse error
         */
        public long getLong(long def) {
            return native_get_long(mNativeHandle, def);
        }
        /**
         * @param def default value
         * @return value or {@code def} on parse error
         */
        public boolean getBoolean(boolean def) {
            return native_get_boolean(mNativeHandle, def);
        }

        private Handle(long nativeHandle) {
            mNativeHandle = nativeHandle;
        }
    }
}
