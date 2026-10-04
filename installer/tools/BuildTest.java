import java.io.IOException;
import java.util.Properties;

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
    }
}
