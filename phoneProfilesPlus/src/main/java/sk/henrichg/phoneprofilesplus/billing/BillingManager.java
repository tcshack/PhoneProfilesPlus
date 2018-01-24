package sk.henrichg.phoneprofilesplus.billing;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import sk.henrichg.phoneprofilesplus.DonationFragment;
import sk.henrichg.phoneprofilesplus.PPApplication;

public class BillingManager implements PurchasesUpdatedListener {

    private final BillingClient mBillingClient;
    private final AppCompatActivity mActivity;

    private static String TAG = "BillingManager";

    private static final HashMap<String, List<String>> SKUS;
    static
    {
        SKUS = new HashMap<>();
        SKUS.put(BillingClient.SkuType.INAPP, Arrays.asList("phoneprofilesplus.donation.1",
                "phoneprofilesplus.donation.2", "phoneprofilesplus.donation.3", "phoneprofilesplus.donation.5", "phoneprofilesplus.donation.8",
                "phoneprofilesplus.donation.13", "phoneprofilesplus.donation.20"));
        //SKUS.put(BillingClient.SkuType.SUBS, Arrays.asList("gold_monthly", "gold_yearly"));
    }

    /*
    private static final HashMap<String, List<String>> SKUS_DEBUG;
    static
    {
        SKUS_DEBUG = new HashMap<>();
        SKUS_DEBUG.put(BillingClient.SkuType.INAPP, Arrays.asList("android.test.purchased",
                "android.test.canceled", "android.test.refunded", "android.test.item_unavailable"));
        //SKUS_DEBUG.put(BillingClient.SkuType.SUBS, Arrays.asList("gold_monthly", "gold_yearly"));
    }
    */

    public BillingManager(AppCompatActivity activity) {
        PPApplication.logE(TAG, "start client");
        mActivity = activity;
        mBillingClient = BillingClient.newBuilder(mActivity).setListener(this).build();
        startServiceConnectionIfNeeded(null);
    }

    private DonationFragment getFragment() {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        return (DonationFragment) fragmentManager.findFragmentByTag("donationFragment");
    }

    private void startServiceConnectionIfNeeded(final Runnable executeOnSuccess) {
        if (mBillingClient.isReady()) {
            if (executeOnSuccess != null) {
                executeOnSuccess.run();
            }
        } else {
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponse) {
                    if (billingResponse == BillingClient.BillingResponse.OK) {
                        Log.i(TAG, "onBillingSetupFinished() response: " + billingResponse);

                        if (executeOnSuccess == null) {
                            getFragment().handleManagerAndUiReady();
                        }

                        if (executeOnSuccess != null) {
                            executeOnSuccess.run();
                        }
                    } else {
                        Log.w(TAG, "onBillingSetupFinished() error code: " + billingResponse);
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                    Log.w(TAG, "onBillingServiceDisconnected()");
                }
            });
        }
    }

    @Override
    public void onPurchasesUpdated(@BillingClient.BillingResponse int responseCode,
                                   List<Purchase> purchases) {
        PPApplication.logE(TAG, "onPurchasesUpdated() response: " + responseCode);
        if (responseCode == BillingClient.BillingResponse.OK) {
            for (Purchase purchase : purchases) {
                consumePurchase(purchase);
            }
        }
        else {
            getFragment().displayAnErrorIfNeeded(responseCode);
        }
    }

    public List<String> getSkus(/*boolean release, */@BillingClient.SkuType String type) {
        //if (release)
            return SKUS.get(type);
        //else
        //    return SKUS_DEBUG.get(type);
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final String itemType,
                                     final List<String> skuList, final SkuDetailsResponseListener listener) {
        // Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                        .setSkusList(skuList).setType(itemType).build();
                mBillingClient.querySkuDetailsAsync(skuDetailsParams,
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(int responseCode,
                                                             List<SkuDetails> skuDetailsList) {
                                listener.onSkuDetailsResponse(responseCode, skuDetailsList);
                            }
                        });
            }
        };

        // If Billing client was disconnected, we retry 1 time and if success, execute the query
        startServiceConnectionIfNeeded(executeOnConnectedService);
    }

    public void startPurchaseFlow(final String skuId, final String billingType) {
        // Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setType(billingType)
                        .setSku(skuId)
                        .build();
                int responseCode = mBillingClient.launchBillingFlow(mActivity, billingFlowParams);
                PPApplication.logE(TAG, "startPurchaseFlow responseCode="+responseCode);
                getFragment().displayAnErrorIfNeeded(responseCode);
            }
        };

        // If Billing client was disconnected, we retry 1 time and if success, execute the query
        startServiceConnectionIfNeeded(executeOnConnectedService);
    }

    private void consumePurchase(final Purchase purchase) {
        // Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                mBillingClient.consumeAsync(purchase.getPurchaseToken(),
                        new ConsumeResponseListener() {
                            @Override
                            public void onConsumeResponse(@BillingClient.BillingResponse int responseCode,
                                                          String outToken) {
                                PPApplication.logE(TAG, "onConsumeResponse() response: " + responseCode);
                                /*if (responseCode == BillingClient.BillingResponse.OK) {
                                    // Handle the success of the consume operation.
                                    // For example, increase the number of player's coins,
                                    // that provide temporary benefits
                                }*/
                            }
                        }
                );
            }
        };

        // If Billing client was disconnected, we retry 1 time and if success, execute the query
        startServiceConnectionIfNeeded(executeOnConnectedService);
    }

    public void destroy() {
        mBillingClient.endConnection();
    }

}