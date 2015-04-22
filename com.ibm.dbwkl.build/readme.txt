README

Before building any of the projects:

1. Create a build definition for each of the build_*.xml files
		Right-Click on the XML --> Run As --> External Tools Configuration
2. In each of these build definitions set the following environment variables
		ANT_HOME				C:\Program Files (x86)\IBM\SDPShared\plugins\org.apache.ant_1.7.1.v20100518-1145 
								(path to your IDEs built in ant version (at least ANT 1.7.1))
		STAF_INSTANCE_NAME		e.g. STAF3410 or STAF 
								(name of the STAF instance that you specified during STAF install (just "STAF" as default))
		STAFCONVDIR				C:\STAF\codepage (usually something similar to this path)
								or 
								C:\Program Files (x86)\IBM\STAF3410\codepage
								(path of the STAF codepage directory) 
3. In the Eclipse preferences, go to 
		Ant --> Runtime --> Properties
   and create the following property
   		staf.path 				C:/Program Files (x86)/IBM/STAF3410/
   								(path to your STAF install directory)
   		db2wkl.build.version 	The version number that is used for the official build script (build_official.xml).
   								The default is vBeta which should be used for all non-offical builds. An official
   								build is for example v4.1. The build script will automatically append the time stamp
   								to separate the builds.
   		db2wkl.deploy.build		The exact version to be deployed when using the deploy task. E.g.
   								v4.1.20140612-161245. Note that for deployment, which is just used internally, you need
   								to have the Deploy package. Neither the release/test or any other package will be used.
   		db2wkl.deploy.target	The target host name / IP adress where to deploy DB2WKL to
   		db2wkl.input.jdbc		Path to a directory that contains the JDBC libraries. This is required for deployment.
   		db2wkl.output			The temporary output directory that is used during build of dev and official drivers.
   								Additionally, deploy will look into that folder to get the driver to deploy.
   														
4. In the Eclipse preferences, go to 
		Ant --> Runtime --> Classpath --> Global Entries
	and select the button "Add JARs..." to add
		ant-contrib.jar from DB2WorkloadService/libs
5. In the Eclipse preferences (same as above), go to 
		Ant --> Runtime --> Classpath --> Global Entries
	and select the button "Add External JARs..." to add
		JSTAF.jar !!AND!! STAFAnt.jar from your STAF installation directory (default C:\STAF\bin)
   						