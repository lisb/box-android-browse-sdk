[box/box-android-browse-sdk](https://github.com/box/box-android-browse-sdk) のフォーク。

[box/box-android-browse-sdk](https://github.com/box/box-android-browse-sdk) は deprecated になっているので必要な修正や更新を独自に実施。

### 開発用設定

サンプルアプリを実行する場合は `local.properties` に以下を指定。

```
box.client_id=${BoxのOAuthのクライアントID}
box.client_secret=${BoxのOAuthのクライアントシークレット}
box.redirect_url=${BoxのOAuthのリダイレクト先}
```

GitHub Repositories にライブラリを配置する場合は `local.properties`　に以下を指定。

```
gpr.user=${GitHubのメールアドレス}
gpr.key=${GitHubのアクセストークン}
```