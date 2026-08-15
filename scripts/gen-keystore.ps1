# CMAL keystore 生成与 Base64 转换脚本
# 使用前请确保已安装 JDK（包含 keytool）

# 1. 生成 keystore（自定义别名与密码）
keytool -genkeypair -v `
  -keystore cmal-release.jks `
  -alias cmal `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass your_store_password `
  -keypass your_key_password `
  -dname "CN=cszy-app, OU=CMAL, O=cszy-app, L=City, ST=State, C=CN"

Write-Host ""
Write-Host "keystore 已生成: cmal-release.jks"
Write-Host "请牢记 storepass / keypass / alias"
Write-Host ""

# 2. 生成 Base64（用于 GitHub Secrets: KEYSTORE_BASE64）
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("cmal-release.jks"))
$b64 | Out-File -FilePath "cmal-release.b64" -Encoding ascii
Write-Host "Base64 已写入 cmal-release.b64（内容较长，整段复制即可）"

# 3. 提示创建 keystore.properties（本机构建使用）
$props = @"
storeFile=cmal-release.jks
storePassword=your_store_password
keyAlias=cmal
keyPassword=your_key_password
"@
$props | Out-File -FilePath "..\keystore.properties" -Encoding ascii
Write-Host ""
Write-Host "已生成 keystore.properties（请将密码替换为实际值，此文件已被 .gitignore 忽略，不会提交）"
