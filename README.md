# Google Services Java Plugin
A tool to generate Google Services Resource File(values.xml) from Google Services JSON File(google-services.json).

The projects is migrated from the official [Google Services Gradle Plugin](https://github.com/google/play-services-plugins/tree/master/google-services-plugin) Repository and is not being officially maitaind by Google. The project aims to help open source community. Thus owner of this repository is ready to resolve Copyright Issues if there are any.



**Usage:**
1. Place `google-services.json` file in the root directory.
2. Run commands the following commands one by one:

      ```
      javac -cp ".;libs/gson-2.8.7.jar;libs/guava-27.0.1-jre.jar" gs.java
      ```
      
      ```
      java -cp ".;libs/gson-2.8.7.jar;libs/guava-27.0.1-jre.jar" gs
      ```
      
3. After running the above commands, a file `values.xml` will be created at `Interm/values/` directory. Make sure these folders exists before running the above commands.



**Libraries Used:**

libs/gson-2.8.7.jar

libs/guava-27.0.1-jre.jar




**Development:**

Feel free to contribute to the project by opening issues and creating pull requests.

