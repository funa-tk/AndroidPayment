package com.funa.androidpayment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient
    private val productIds = arrayOf(
        "testitem1"
    )
    private val productMap = mutableMapOf<String, ProductDetails>()
    private val purchasedProduct = mutableListOf<String>()

    private fun getQueryProductDetailsParams(productId: String): QueryProductDetailsParams {
        return QueryProductDetailsParams.newBuilder()
            .setProductList(
                ImmutableList.of(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
    }

    private suspend fun getPurchaseItem(productId: String): List<ProductDetails> {
        return suspendCoroutine { cont ->
            this.billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        billingClient.queryProductDetailsAsync(getQueryProductDetailsParams(productId)) { billingResult, productDetailsList ->
                            if (billingResult.responseCode == BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                                cont.resume(productDetailsList)
                            } else {
                                cont.resumeWith(Result.failure(Exception()))
                            }
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                }
            })
        }
    }

    private suspend fun handlePurchases(purchases: MutableList<Purchase>?) {
        for (purchase in purchases!!) {
            handlePurchase(purchase)
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchasedProduct.contains(purchase.orderId)) {
            return
        }
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val consumeResult = withContext(Dispatchers.IO) {
            this@MainActivity.billingClient.consumePurchase(consumeParams)
        }
        if (consumeResult.billingResult.responseCode == BillingResponseCode.OK) {
            purchasedProduct.add(purchase.orderId!!)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases -> // PurchasesUpdatedListener
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    // 購入成功
                    for (purchase in purchases!!) {
                        val textViewReceipt = findViewById<TextView>(R.id.textViewReceipt)
                        val textViewSig = findViewById<TextView>(R.id.textViewSig)
                        textViewReceipt.text = "Receipt: " + purchase.originalJson
                        textViewSig.text = "Signature: " + purchase.signature
                        Log.i("Receipt", "Receipt: " + purchase.originalJson)
                        Log.i("Signature", "Signature: " + purchase.signature)
                        MainScope().launch {
                            handlePurchase(purchase)
                        }
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        MainScope().launch {
            val spinner = findViewById<Spinner>(R.id.spinner)
            val list = ArrayList<String>()

            for (productId in productIds) {
                val item = getPurchaseItem(productId)
                this@MainActivity.productMap[item[0].title] = item[0]
                list.add(item[0].title)
            }

            val adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                list
            )
            spinner.adapter = adapter
        }

        // 購入ボタン
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val spinner = findViewById<Spinner>(R.id.spinner)
            val product: ProductDetails? = productMap[spinner.selectedItem]

            product?.let {
                val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(it)
                            .build()
                    )
                ).build()
                if (this.billingClient.isReady) {
                    val billingResult = this.billingClient.launchBillingFlow(this, params)
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.e("launchBillingFlow", "エラー：購入できません")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        this.billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                MainScope().launch {
                    handlePurchases(purchases)
                }
            }
        }
    }

}
