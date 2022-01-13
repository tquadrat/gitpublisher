# gitpublisher

I still have a strong preference for Subversion as my source code repository, and for my (private) projects, I have set up a private Subversion server somewhere in the internet.

But for publishing those projects and the related sources to the public domain, nowadays Git is the tool of choice and GitHub is one of the platforms to be.

As the answer to this requirement, I created the Gradle plugin `gitpublisher` that you will find here.

Basically, it works like this:

 1. Get the remote Git repository on your hard drive by cloning it into a temporary folder.
 2. Copy the files from your project into that working copy.
 3. Commit the changes.
 4. Push the repository.
 5. Cleanup by removing the temporary folder.

## Documentation
The [Javadoc Reference Documentation](https://tquadrat.github.io/gitpublisher/javadoc/index.html) gives some more insight.

## Configuration

 - Add the following lines to your `build.gradle`:
    ```
    plugins {
        id  'org.tquadrat.foundation.gradle.gitpublisher'
    }
    
    tasks.named( 'publishToGIT' ) {
        commitMessage = "The commit message; obviously, it should be something generated and meaningful"
        username = "Your username for the Git repository; for GitHub, this is 'Token'"
        password = "Your password for the Git repository; for GitHub, this is your access token"
        sourcesList = [
            "# The source files to publish; for the format of these Strings, refer",
            "# to the JavaDoc for java.nio.file.FileSystem#getPathMatcher.",
            "src/**"
        ]
        remoteRepositoryURI = new URI( "The URI for the access to your Git repository" )
        
        //---* Optional Parameters *-----------------------------------------------
        debugFlag = false // Set this to true to get some debug output
        dryRunFlag = false // Set this to true if you do not want to update the repository
        credentials = new CredentialsProvider() // Use this instead of username and
            // password when your Git repository requires some more sophisticated 
            // login parameters
        workFolder = new File( """
            The work folder for the plugin; it will not be removed after the plugin 
            has finished. The default is the directory $PROJECT/gitpublishwork.
            """ )
        localRepositoryFolder = """
            This the name for the temporary repository; it will be created in the
            workFolder and usually, it will be removed in the end. If not explicitly
            set, it will be a temporary directory with a name that is prefixed with
            "tempGit_". 
            """
        ignoresList = [
            "# This is a list of files that should be excluded. It has the same",
            "# structure as sourcesList, above.",
            ".git/**"
        ]
        mustCleanupFlag = true // If set to false, the localRepositoryFolder will
            // be kept after the plugin has finished.
        metaDir = new File( """
            A folder with additional files; see below! The default is $PROJECT/gitMeta.
            """ ) 
        javadocLocation = new File( """
            If specified, the contents of this folder will be copied to the folder
            'javadoc' in the respository. A relative path will be resolved against
            the $PROJECT folder.
            """  )
    } 
    ```

## The `metaDir` Folder

Usually, you have several files and folders in the root of your project that you do not want to publish – at least this is true for me!

A solution could be to add these files explicitly to the `sourcesList`; or to add the root folder as a whole, and list the unwanted files to the `ignoresList`.

But than I found that sometimes I want to publish another version of a file to the public Git repository than that I use when I build the project in my own environment. So I introduced the `metaDir` folder: all files in that folder are copied to the root of the repository.