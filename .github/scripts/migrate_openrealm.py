#!/usr/bin/env python3
from pathlib import Path
import json, re, shutil, subprocess

ROOT = Path(".").resolve()
TEXT_EXT = {".kt",".java",".kts",".gradle",".xml",".json",".toml",".properties",".md",".txt",".yml",".yaml",".sh",".c",".cc",".cpp",".h",".hpp",".mk",".pro",".rules"}
SKIP = {".git",".gradle","build",".idea"}

def read(p):
    try:
        return p.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return None

def write(p, s):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(s, encoding="utf-8")

def all_text():
    for p in ROOT.rglob("*"):
        if p.is_file() and p.suffix.lower() in TEXT_EXT and not any(x in SKIP for x in p.parts) and ".github" not in p.parts and p.name != "LICENSE":
            yield p

old = ROOT / "ZalithLauncher"
new = ROOT / "OpenRealmLauncher"
if old.exists() and not new.exists():
    old.rename(new)

repls = [
    ("com.movtery.zalithlauncher", "dev.openrealm.launcher"),
    ("com/movtery/zalithlauncher", "dev/openrealm/launcher"),
    ("Java_com_movtery_zalithlauncher", "Java_dev_openrealm_launcher"),
    ("com_movtery_zalithlauncher", "dev_openrealm_launcher"),
    ("Zalith Launcher 2", "OpenRealm Launcher"),
    ("ZalithLauncher2", "OpenRealmLauncher"),
    ("Zalith Launcher", "OpenRealm Launcher"),
    ("ZalithLauncher", "OpenRealmLauncher"),
    ("zalithlauncher", "openrealmlauncher"),
    ("https://github.com/ZalithLauncher/ZalithLauncher2", "https://github.com/Rushabh-00/OpenRealmLauncher"),
    ("https://github.com/ZalithLauncher/Zalith-Info", "https://github.com/Rushabh-00/OpenRealmLauncher"),
    ("Zalith-Info", "OpenRealmLauncher"),
]
for p in list(all_text()):
    s = read(p)
    if s is None:
        continue
    t = s
    for a, b in repls:
        t = t.replace(a, b)
    if t != s:
        write(p, t)

for base in (ROOT / "OpenRealmLauncher", ROOT / "LWJGL"):
    if not base.exists():
        continue
    for p in sorted(base.rglob("zalithlauncher"), key=lambda x: len(x.parts), reverse=True):
        if p.is_dir() and p.parent.name == "movtery" and p.parent.parent.name == "com":
            target = p.parent.parent.parent / "dev" / "openrealm" / "launcher"
            if not target.exists():
                target.parent.mkdir(parents=True, exist_ok=True)
                p.rename(target)

for p in sorted(ROOT.rglob("*"), key=lambda x: len(x.parts), reverse=True):
    if not p.exists() or any(x in SKIP for x in p.parts):
        continue
    name = p.name
    n = name.replace("ZalithLauncher2","OpenRealmLauncher").replace("ZalithLauncher","OpenRealmLauncher").replace("zalithlauncher","openrealmlauncher")
    if n != name:
        target = p.with_name(n)
        if not target.exists():
            p.rename(target)

settings = ROOT / "settings.gradle.kts"
s = read(settings) or ""
s = s.replace('rootProject.name = "ZalithLauncher"', 'rootProject.name = "OpenRealmLauncher"')
s = s.replace('include(":ZalithLauncher")', 'include(":OpenRealmLauncher")')
write(settings, s)

write(ROOT / "OpenRealmLauncher/gradle.properties", """launcher_name=OpenRealmLauncher
launcher_app_name=OpenRealm Launcher
launcher_short_name=ORL
url_home=https://github.com/Rushabh-00/OpenRealmLauncher
launcher_version_code=1
launcher_version_name=1.0.0
""")

app_lang = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/setting/enums/AppLanguage.kt"
if app_lang.exists():
    app_lang.unlink()

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/setting/AllSettings.kt"
s = read(p) or ""
s = s.replace("import dev.openrealm.launcher.setting.enums.AppLanguage\n", "")
s = s.replace('''    /**
     * 启动器语言
     */
    val launcherLanguage = enumSetting("launcherLanguage", AppLanguage.FOLLOW_SYSTEM)
''', "")
if "AppLanguage" in s or "launcherLanguage" in s:
    raise SystemExit("English-only migration failed: AppLanguage remains in AllSettings.kt")
write(p, s)
p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/ui/screens/content/settings/LauncherSettingsScreen.kt"
s = read(p) or ""
s = s.replace("import dev.openrealm.launcher.setting.enums.AppLanguage\n", "")
s = s.replace("import dev.openrealm.launcher.setting.enums.applyLanguage\n", "")
language_card = '''                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherLanguage,
                        items = AppLanguage.entries,
                        title = stringResource(R.string.settings_launcher_language),
                        getItemText = { stringResource(it.textRes) },
                        onValueChange = {
                            applyLanguage(it)
                        }
                    )
'''
if language_card not in s:
    raise SystemExit("English-only migration failed: launcher language card not found")
s = s.replace(language_card, "", 1)
s = s.replace("import dev.openrealm.launcher.utils.isChinaMainland\n", "")
if "AppLanguage" in s or "applyLanguage" in s or "AllSettings.launcherLanguage" in s:
    raise SystemExit("English-only migration failed: language references remain in LauncherSettingsScreen.kt")
s = s.replace('position = if (isChinaMainland) {
                            CardPosition.Middle
                        } else {
                            CardPosition.Top
                        },', 'position = CardPosition.Middle,')
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/game/account/AccountsManager.kt"
s = read(p) or ""
s = s.replace("import dev.openrealm.launcher.path.PathManager\n", "").replace("import dev.openrealm.launcher.utils.isInGreaterChina\n", "")
s = re.sub(r'    private fun refreshCurrentAccountState\(\) \{.*?\n    \}\n\n    private fun checkLimit\(\): Boolean \{.*?\n    \}\n', '''    private fun refreshCurrentAccountState() {
        val currentAccount = getCurrentAccount()
        _currentAccountFlow.update { currentAccount }
        _isOffline.update { false }
    }
''', s, count=1, flags=re.S)
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/utils/LocalUtils.kt"
s = read(p) or ""
s = re.sub(r'\nfun isInGreaterChina\(\): Boolean \{.*?\n\}\n\n/\*\*\s*\n \* 判断当前时区是否属于中国\s*\n \*/\s*\nprivate fun isChinaTimeZone\(\): Boolean \{.*?\n\}\n', '\n', s, flags=re.S)
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/game/addons/mirror/_SourceUtils.kt"
s = read(p) or ""
if "import dev.openrealm.launcher.utils.isChinaMainland" not in s:
    s = s.replace("import dev.openrealm.launcher.utils.logging.Logger\n", "import dev.openrealm.launcher.utils.isChinaMainland\nimport dev.openrealm.launcher.utils.logging.Logger\n")
s = s.replace("resolveMirrorPriority(AllSettings.gameDownloadSource.getValue(), mainland = true)", "resolveMirrorPriority(AllSettings.gameDownloadSource.getValue(), mainland = isChinaMainland())")
write(p, s)

for rel in [
    "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/game/addons/mirror/BMCLAPI.kt",
    "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/game/download/assets/platform/mcim/MCIMMirror.kt"
]:
    p = ROOT / rel
    s = read(p) or ""
    s = s.replace("    if (!isChinaMainland()) return listOf(this)\n\n", "")
    s = s.replace("    if (!isChinaMainland()) return toList()\n\n", "")
    s = s.replace("mainland = true", "mainland = isChinaMainland()")
    write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/game/download/assets/platform/_PlatformSearch.kt"
s = read(p) or ""
if "import dev.openrealm.launcher.BuildKeys" not in s:
    s = s.replace("import android.util.Log\n", "import android.util.Log\nimport dev.openrealm.launcher.BuildKeys\n")
s = s.replace("enabledMirror: Boolean = isChinaMainland()", "enabledMirror: Boolean = true")
source_old = '''    val source = resolveMirrorPriority(AllSettings.assetPlatformSource.getValue(), mainland = enabledMirror)
    val mirrorSource = mirrorCurseForgeSearcher.takeIf { enabledMirror }
    return when (source) {
        MirrorPriority.OFFICIAL -> listOf(curseForgeSearcher)
        MirrorPriority.MIRROR_FIRST ->
            listOfNotNull(mirrorSource, curseForgeSearcher)
    }
}'''
source_new = '''    if (BuildKeys.CURSEFORGE_API.isBlank()) {
        return listOf(curseForgeSearcher, mirrorCurseForgeSearcher)
    }
    val source = resolveMirrorPriority(
        AllSettings.assetPlatformSource.getValue(),
        mainland = enabledMirror && isChinaMainland()
    )
    val mirrorSource = mirrorCurseForgeSearcher.takeIf { enabledMirror }
    return when (source) {
        MirrorPriority.OFFICIAL -> listOf(curseForgeSearcher)
        MirrorPriority.MIRROR_FIRST ->
            listOfNotNull(mirrorSource, curseForgeSearcher)
    }
}'''
s = s.replace(source_old, source_new, 1)
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/game/download/assets/platform/curseforge/models/CurseForgeFile.kt"
s = read(p) or ""
if "import dev.openrealm.launcher.BuildKeys" not in s:
    s = s.replace("import dev.openrealm.launcher.game.download.assets.platform.Platform\n", "import dev.openrealm.launcher.BuildKeys\nimport dev.openrealm.launcher.game.download.assets.platform.Platform\n")
new_url = '''fun CurseForgeFile.fixedFileUrl(): String? {
    val direct = downloadUrl?.takeIf {
        BuildKeys.CURSEFORGE_API.isNotBlank() || !it.contains("edge.forgecdn.net")
    }
    if (direct != null) return direct
    if (BuildKeys.CURSEFORGE_API.isBlank() && modId > 0) {
        return "https://www.curseforge.com/api/v1/mods/" + modId + "/files/" + id + "/download"
    }
    return fileName?.let {
        "https://edge.forgecdn.net/files/" + (id / 1000) + "/" + (id % 1000) + "/" + it
    }
}
'''
s = re.sub(r'fun CurseForgeFile\.fixedFileUrl\(\): String\? \{.*?\n\}', new_url.rstrip(), s, count=1, flags=re.S)
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/path/UrlManager.kt"
s = read(p) or ""
s = re.sub(r'^const val URL_PROJECT_INFO.*\n', '', s, flags=re.M)
s = re.sub(r'^const val URL_WEBLATE.*\n', '', s, flags=re.M)
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/viewmodel/LauncherUpgradeViewModel.kt"
s = read(p) or ""
s = s.replace("import dev.openrealm.launcher.path.URL_PROJECT_INFO\n", "")
s = s.replace("import dev.openrealm.launcher.upgrade.GithubContentApi\n", "")
s = s.replace("import dev.openrealm.launcher.utils.string.decodeBase64\n", "")
s = s.replace('''private const val LATEST_VERSION = "latest_version_md.json"
private const val LATEST_API_URL = "$URL_PROJECT_INFO/$LATEST_VERSION"
private const val LATEST_API_CHINESE_URL = "https://repo.miawa.cn/zalith-info/v2/$LATEST_VERSION"
''','''private const val LATEST_API_URL =
    "https://raw.githubusercontent.com/Rushabh-00/OpenRealmLauncher/main/update/latest_version_md.json"
''')
start = s.find("    private suspend fun fetchRemoteData(): RemoteData?")
if start >= 0:
    end = s.find("\n    /**", start)
    if end >= 0:
        s = s[:start] + '''    private suspend fun fetchRemoteData(): RemoteData? {
        return withContext(Dispatchers.IO) {
            runCatching {
                withRetry(logTag = "LauncherUpgrade", maxRetries = 2) {
                    GLOBAL_CLIENT.get(LATEST_API_URL).safeBodyAsJson<RemoteData>()
                }
            }.getOrElse { e ->
                Logger.warning(TAG, "Failed to check for launcher upgrade!", e)
                null
            }
        }
    }
''' + s[end:]
s = s.replace("import java.util.Locale\n", "")
if "URL_PROJECT_INFO" in s or "LATEST_API_CHINESE_URL" in s or "GithubContentApi" in s or "decodeBase64" in s:
    raise SystemExit("OpenRealm updater migration failed: old feed implementation remains")
write(p, s)
p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/ui/screens/content/settings/AboutInfoScreen.kt"
s = read(p) or ""
s = s.replace("import dev.openrealm.launcher.path.URL_WEBLATE\n", "")
s = re.sub(r'\n\s*LinkIconItem\(\s*icon = painterResource\(R\.drawable\.img_platform_weblate\),.*?openLink = \{ openLink\(URL_WEBLATE\) \}\s*\)\s*', '\n', s, flags=re.S)
s = s.replace('''                        ButtonIconItem(
                            icon = painterResource(R.drawable.img_avatar_movtery),''','''                        Text(
                            text = stringResource(R.string.about_unofficial_fork),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        ButtonIconItem(
                            icon = painterResource(R.drawable.img_avatar_movtery),''',1)
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/res/values/strings.xml"
s = read(p) or ""
if 'name="about_unofficial_fork"' not in s:
    s = s.replace('    <string name="about_launcher_title">About Launcher</string>', '    <string name="about_launcher_title">About Launcher</string>\n    <string name="about_unofficial_fork">Unofficial modified fork. Not affiliated with the original project.</string>')
for marker in ["about_acknowledgements_weblate_community","about_acknowledgements_weblate_community_text","settings_launcher_language"]:
    s = "\n".join(line for line in s.splitlines() if marker not in line) + "\n"
write(p, s)

for name in ["values-ar","values-es","values-in","values-ja","values-ko","values-pt","values-pt-rBR","values-ru","values-th","values-tr","values-vi","values-zh-rCN","values-zh-rTW"]:
    for p in ROOT.rglob(name):
        if p.is_dir() and "src" in p.parts and "res" in p.parts:
            shutil.rmtree(p)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/utils/device/DisplayRefreshRateController.kt"
write(p, '''package dev.openrealm.launcher.utils.device

import android.app.Activity
import android.os.Build
import dev.openrealm.launcher.utils.logging.Logger

object DisplayRefreshRateController {
    private const val TAG = "DisplayRefreshRate"

    fun apply(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display ?: activity.windowManager.defaultDisplay
            } else {
                activity.windowManager.defaultDisplay
            }
            val target = display.supportedModes
                ?.filter { it.refreshRate.isFinite() }
                ?.maxByOrNull { it.refreshRate }
                ?: return
            @Suppress("DEPRECATION")
            val attrs = activity.window.attributes
            if (attrs.preferredDisplayModeId == target.modeId) return
            attrs.preferredDisplayModeId = target.modeId
            activity.window.attributes = attrs
            Logger.info(TAG, "Display refresh request: " + target.refreshRate + "Hz")
        }.onFailure {
            Logger.warning(TAG, "Unable to request display refresh rate", it)
        }
    }
}
''')

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/ui/activities/MainActivity.kt"
s = read(p) or ""
write(p, s)

p = ROOT / "OpenRealmLauncher/src/main/java/dev/openrealm/launcher/ui/base/BaseAppCompatActivity.kt"
s = read(p) or ""
if "DisplayRefreshRateController" not in s:
    s = s.replace(
        "import dev.openrealm.launcher.BuildKeys\n",
        "import dev.openrealm.launcher.BuildKeys\nimport dev.openrealm.launcher.utils.device.DisplayRefreshRateController\n",
        1
    )
s = s.replace(
    "        super.onCreate(savedInstanceState)\n\n        refreshContext(this)",
    "        super.onCreate(savedInstanceState)\n        DisplayRefreshRateController.apply(this)\n\n        refreshContext(this)",
    1
)
s = s.replace(
    "        super.onResume()\n        loadAllSettings(this, true)",
    "        super.onResume()\n        DisplayRefreshRateController.apply(this)\n        loadAllSettings(this, true)",
    1
)
write(p, s)

write(ROOT / "update/latest_version_md.json", json.dumps({
    "code": 0, "version": "0.0.0", "created_at": "1970-01-01T00:00:00Z",
    "files": [], "default_body": {"language":"en","markdown":"OpenRealm Launcher release metadata will be published here."}, "bodies": []
}, indent=2) + "\n")

gi = ROOT / ".gitignore"
s = read(gi) or ""
for line in ["*.jks",".store_password.txt",".key_password.txt",".curseforge_api.txt","openrealm-release.jks"]:
    if line not in s.splitlines():
        s += line + "\n"
write(gi, s)

for rel in ["OpenRealmLauncher/zalith_launcher.jks","OpenRealmLauncher/zalith_launcher_debug.jks","OpenRealmLauncher/.store_password.txt","OpenRealmLauncher/.key_password.txt","OpenRealmLauncher/.curseforge_api.txt"]:
    p = ROOT / rel
    if p.exists():
        p.unlink()

p = ROOT / "README.md"
s = read(p) or ""
if "Upstream attribution" not in s:
    s = s.rstrip() + "\n\n## Upstream attribution\n\nOpenRealm Launcher is an unofficial modified fork of the GPL-licensed ZalithLauncher2 project. Upstream copyright and license notices are retained where applicable.\n"
write(p, s)

# Stage the transformed tree; the workflow performs the final audit after removing this migration script.
subprocess.run(["git","add","-A"], cwd=ROOT, check=True)
print("OpenRealm migration transformation complete")
