import java.io.IOException;
import java.util.Properties;

/** 文件名可编辑，但不得借模板写到发布目录之外。 */
class BuildTest {
    public static void main(String[] args) throws Exception {
        Properties p=new Properties();p.setProperty("pack.version","3.21.7");p.setProperty("translation.version","3.7");
        p.setProperty("export.filename","宝藏猎人3汉化安装器-{modpackVersion}-VM汉化组-V{translationVersion}-{exportSerial}.jar");
        if(!Build.outputName(p,"1").equals("宝藏猎人3汉化安装器-3.21.7-VM汉化组-V2.7-1.jar"))throw new AssertionError("模板展开错误");
        for(String value:new String[]{"../escape.jar","C:\\escape.jar","bad/{exportSerial}.jar","{unknown}.jar","file.txt","bad\nfile.jar"}) {
            p.setProperty("export.filename",value);
            try { Build.outputName(p,"1");throw new AssertionError("不应接受："+value); } catch(IOException expected) {}
        }
        p.setProperty("export.filename","ok-{translationVersion}.jar");p.setProperty("translation.version","../../bad");
        try { Build.outputName(p,"1");throw new AssertionError("版本值不可逃逸目录"); } catch(IOException expected) {}
        System.out.println("PASS: 8 项导出名称与路径校验");
    }
}
