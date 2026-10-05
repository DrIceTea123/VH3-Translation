import java.io.IOException;
import java.util.Properties;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** 文件名可编辑，但不得借模板写到发布目录之外。 */
class BuildTest {
    public static void main(String[] args) throws Exception {
        // 独立示例，不随 installer.properties 的发布版本修改。
        Properties p=new Properties();p.setProperty("pack.version","9.8.7");p.setProperty("translation.version","6.5");
        p.setProperty("export.filename","宝藏猎人3汉化安装器-{modpackVersion}-VM汉化组-V{translationVersion}-{exportSerial}.jar");
        String actual=Build.outputName(p,"42");
        if(!actual.equals("宝藏猎人3汉化安装器-9.8.7-VM汉化组-V6.5-42.jar"))throw new AssertionError("模板展开错误："+actual);
        for(String value:new String[]{"../escape.jar","C:\\escape.jar","bad/{exportSerial}.jar","{unknown}.jar","file.txt","bad\nfile.jar"}) {
            p.setProperty("export.filename",value);
            try { Build.outputName(p,"1");throw new AssertionError("不应接受："+value); } catch(IOException expected) {}
        }
        p.setProperty("export.filename","ok-{translationVersion}.jar");p.setProperty("translation.version","../../bad");
        try { Build.outputName(p,"1");throw new AssertionError("版本值不可逃逸目录"); } catch(IOException expected) {}
        System.out.println("PASS: 8 项导出名称与路径校验");
        Path project = Path.of(args[0]);
        var launchers = Build.launcherFiles(project.resolve("launchers"));
        if (launchers.size() != 3) throw new AssertionError("应只导出三个独立启动脚本");
        for (String script : new String[]{"macOS系统点我启动.command", "Linux系统点我启动.sh"}) {
            String text = new String(launchers.get(script), StandardCharsets.UTF_8);
            if (!text.startsWith("#!/bin/sh\n") || text.contains("\r")) throw new AssertionError("Unix 脚本编码/换行错误");
        }
        String cmd = new String(launchers.get("windows系统点我启动.cmd"), StandardCharsets.UTF_8);
        if (!cmd.startsWith("@echo off\r\n") || !cmd.contains("# POWERSHELL_BEGIN\r\n")) throw new AssertionError("Windows 独立脚本缺少内置启动逻辑");
        System.out.println("PASS: 三个独立启动脚本的编码与换行");
    }
}
