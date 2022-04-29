/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.server.nearby.common.bluetooth.fastpair;

import android.annotation.WorkerThread;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.nearby.common.bluetooth.BluetoothException;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/** Abstract class for pairing or connecting via FastPair. */
public abstract class FastPairConnection {
    @Nullable protected OnPairedCallback mPairedCallback;
    @Nullable protected OnGetBluetoothAddressCallback mOnGetBluetoothAddressCallback;
    @Nullable protected PasskeyConfirmationHandler mPasskeyConfirmationHandler;
    @Nullable protected FastPairSignalChecker mFastPairSignalChecker;
    @Nullable protected Consumer<Integer> mRescueFromError;
    @Nullable protected Runnable mPrepareCreateBondCallback;
    protected boolean mPasskeyIsGotten;

    /** Sets a callback to be invoked once the device is paired. */
    public void setOnPairedCallback(OnPairedCallback callback) {
        this.mPairedCallback = callback;
    }

    /** Sets a callback to be invoked while the target bluetooth address is decided. */
    public void setOnGetBluetoothAddressCallback(OnGetBluetoothAddressCallback callback) {
        this.mOnGetBluetoothAddressCallback = callback;
    }

    /** Sets a callback to be invoked while handling the passkey confirmation. */
    public void setPasskeyConfirmationHandler(
            PasskeyConfirmationHandler passkeyConfirmationHandler) {
        this.mPasskeyConfirmationHandler = passkeyConfirmationHandler;
    }

    public void setFastPairSignalChecker(FastPairSignalChecker fastPairSignalChecker) {
        this.mFastPairSignalChecker = fastPairSignalChecker;
    }

    public void setRescueFromError(Consumer<Integer> rescueFromError) {
        this.mRescueFromError = rescueFromError;
    }

    public void setPrepareCreateBondCallback(Runnable runnable) {
        this.mPrepareCreateBondCallback = runnable;
    }

    @VisibleForTesting
    @Nullable
    public Runnable getPrepareCreateBondCallback() {
        return mPrepareCreateBondCallback;
    }

    /**
     * Sets the fast pair history for identifying whether or not the provider has paired with the
     * primary account on other phones before.
     */
    @WorkerThread
    public abstract void setFastPairHistory(List<FastPairHistoryItem> fastPairHistoryItem);

    /** Sets the device name to the Provider. */
    public abstract void setProviderDeviceName(String deviceName);

    /** Gets the device name from the Provider. */
    @Nullable
    public abstract String getProviderDeviceName();

    /**
     * Gets the existing account key of the Provider.
     *
     * @return the existing account key if the Provider has paired with the account, null otherwise
     */
    @WorkerThread
    @Nullable
    public abstract byte[] getExistingAccountKey();

    /**
     * Pairs with Provider. Synchronous: Blocks until paired and connected. Throws on any error.
     *
     * @return the secret key for the user's account, if written
     */
    @WorkerThread
    @Nullable
    public abstract SharedSecret pair()
            throws BluetoothException, InterruptedException, TimeoutException, ExecutionException,
            PairingException, ReflectionException;

    /**
     * Pairs with Provider. Synchronous: Blocks until paired and connected. Throws on any error.
     *
     * @param key can be in two different formats. If it is 16 bytes long, then it is an AES account
     *    key. Otherwise, it's a public key generated by {@link EllipticCurveDiffieHellmanExchange}.
     *    See go/fast-pair-2-spec for how each of these keys are used.
     * @return the secret key for the user's account, if written
     */
    @WorkerThread
    @Nullable
    public abstract SharedSecret pair(@Nullable byte[] key)
            throws BluetoothException, InterruptedException, TimeoutException, ExecutionException,
            PairingException, GeneralSecurityException, ReflectionException;

    /** Unpairs with Provider. Synchronous: Blocks until unpaired. Throws on any error. */
    @WorkerThread
    public abstract void unpair(BluetoothDevice device)
            throws InterruptedException, TimeoutException, ExecutionException, PairingException,
            ReflectionException;

    /** Gets the public address of the Provider. */
    @Nullable
    public abstract String getPublicAddress();


    /** Callback for getting notifications when pairing has completed. */
    public interface OnPairedCallback {
        /** Called when the device at address has finished pairing. */
        void onPaired(String address);
    }

    /** Callback for getting bluetooth address Bisto oobe need this information */
    public interface OnGetBluetoothAddressCallback {
        /** Called when the device has received bluetooth address. */
        void onGetBluetoothAddress(String address);
    }

    /** Holds the exchanged secret key and the public mac address of the device. */
    public static class SharedSecret {
        private final byte[] mKey;
        private final String mAddress;
        private SharedSecret(byte[] key, String address) {
            mKey = key;
            mAddress = address;
        }

        /** Creates Shared Secret. */
        public static SharedSecret create(byte[] key, String address) {
            return new SharedSecret(key, address);
        }

        /** Gets Shared Secret Key. */
        public byte[] getKey() {
            return mKey;
        }

        /** Gets Shared Secret Address. */
        public String getAddress() {
            return mAddress;
        }

        @Override
        public String toString() {
            return "SharedSecret{"
                    + "key=" + Arrays.toString(mKey) + ", "
                    + "address=" + mAddress
                    + "}";
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof SharedSecret) {
                SharedSecret that = (SharedSecret) o;
                return Arrays.equals(this.mKey, that.getKey())
                        && this.mAddress.equals(that.getAddress());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(mKey), mAddress);
        }
    }

    /** Invokes if gotten the passkey. */
    public void setPasskeyIsGotten() {
        mPasskeyIsGotten = true;
    }

    /** Returns the value of passkeyIsGotten. */
    public boolean getPasskeyIsGotten() {
        return mPasskeyIsGotten;
    }

    /** Interface to get latest address of ModelId. */
    public interface FastPairSignalChecker {
        /** Gets address of ModelId. */
        String getValidAddressForModelId(String currentDevice);
    }
}