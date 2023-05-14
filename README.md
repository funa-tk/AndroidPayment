# AndroidPayment

AndroidのIABにおいて、他アプリのレシートと入れ替えると通ってしまうという脆弱性が存在する。
このアプリは、簡単にレシートを取得できるアプリなので、アプリのIABのレシート偽装の脆弱性の診断に便利です。
対策は、ちゃんとレシートの `packageName` を確認することです。

![スクリーンショット](screenshot/screenshot.png)

## 使い方

1. Google Play Consoleにログインして、アプリを登録する（アプリIDを、com.funa.paymentsampleとする)
2. Google Play Consoleの商品で、攻撃対象のアプリの商品名と同じ商品を作成する
    1. 例： powerup.item.100
3. 本アプリの app/build.gradle の applicationId を `com.funa.paymentsample` とする
4. 本アプリの app/src/main/java/com/funa/androidpayment/MainActivity.kt の procuctIds という配列に、先ほどGoogle Play Consoleで登録した商品ID(powerup.item.100)を追記する。
5. Android Studioで開き、ビルド→実行する
6. アプリのリストボックスから購入したい商品名を選択し、購入ボタンをタップすると、レシートが画面に表示されるとともに、logに出力されます。PCに取り込みたい時は、adb logcatで取得すると便利です。

