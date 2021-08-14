package jack.android.gradle.embedfunction.asm;

import org.apache.tools.ant.taskdefs.Tar;

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 * @email bingo110@126.com
 */
public class TargetClass {
    private InternalClass internalClass;

    public TargetClass(){
        internalClass = new InternalClass();
    }

    public void printMessage(){
        System.out.println("Hello from TargetClass");
    }
}
