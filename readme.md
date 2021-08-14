## Readme

Some configuration you may want to know
First is AS do not support the Gradle Runner.

![image1](documents/as_gradle_runner.png)

![image2](documents/idea_gradle_runner.png)

However, We allow to change the configuration manually.

![image3](documents/the_workspace_configuration.png)

Another problem, Since we put our extensions into the global gitignore file. We may want to change the git file status colors.

![image4](documents/file_status_colors.png)


### How to start
* Copy the app_extension to your project.
* Execute the script: app_extension/script/app_extension_install.sh

That's it.

### How to modify your code.
For example change our activity

```
//main:/jack.andorid.embedfunction.InternalClass
//This is a internal class, we should access it in the same package. 
class InternalClass {
    public void testFunction(){
        System.out.println("Hello world");
    }
}

//main:/jack.andorid.embedfunction.MainActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
```

The decorate class

```
//This annotation class tells us which class we want to decorate.
@Decorate(target = MainActivity::class)
class MainActivity_Decorated : AppCompatActivity() {
    //We will overwrite the whole function.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.text_view).setOnClickListener {
            //Noticed here: That we are access the internal class.
            InternalClass().testFunction()
            Toast.makeText(applicationContext,"Test",Toast.LENGTH_SHORT).show()
        }
    }
    
    //This method will be add to the MainActivity
    fun add(){
        InternalClass().testFunction()
    }
}
```

* How to enable the Gradle init-script
    1. Use command parameter for example：gradle --init-script yourdir/init.gradle -q taskName
    2. Put the init.gradle file to USER_HOME/.gradle/.
    3. Put the script file (.gradle) to USER_HOME/.gradle/init.d/.
    4. Put the script file (.gradle_) to GRADLE_HOME/init.d/.

## References
*[asm-transformations](https://lsieun.cn/assets/pdf/asm-transformations.pdf)