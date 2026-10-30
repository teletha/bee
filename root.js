const root = {
	"docs": [],
	"modules": [],
	"packages": [
		"bee",
		"bee.api",
		"bee.util",
		"bee.task",
		"bee.coder"
	],
	"types": [
		{
			"name": "TaskOperations",
			"packageName": "bee",
			"type": "Class"
		},
		{
			"name": "Library",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "License",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "Command",
			"packageName": "bee.api",
			"type": "Annotation"
		},
		{
			"name": "Comment",
			"packageName": "bee.api",
			"type": "Annotation"
		},
		{
			"name": "Loader",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "ProjectSpecific",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "VCS",
			"packageName": "bee.api",
			"type": "AbstractClass"
		},
		{
			"name": "VCS.Release",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "VCS.Commit",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "VCS.Content",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "VCS.Account",
			"packageName": "bee.api",
			"type": "Interface"
		},
		{
			"name": "Project",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "Repository",
			"packageName": "bee.api",
			"type": "Class"
		},
		{
			"name": "Scope",
			"packageName": "bee.api",
			"type": "Enum"
		},
		{
			"name": "JavaCompiler",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "Profiling",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "SemanticVersion",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "SemanticVersion.Bump",
			"packageName": "bee.util",
			"type": "Enum"
		},
		{
			"name": "Terminal",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "Process",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "GithubAPI",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "GithubAPI.Step",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "GithubAPI.Job",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "GithubAPI.Run",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "GithubAPI.Settings",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "Git",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "Git.Tracking",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "Git.Status",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "Java",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "Java.Transporter",
			"packageName": "bee.util",
			"type": "Interface"
		},
		{
			"name": "Java.JVM",
			"packageName": "bee.util",
			"type": "AbstractClass"
		},
		{
			"name": "Inputs",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "ConventionalCommit",
			"packageName": "bee.util",
			"type": "Class"
		},
		{
			"name": "UserInterface",
			"packageName": "bee",
			"type": "AbstractClass"
		},
		{
			"name": "Bee",
			"packageName": "bee",
			"type": "Class"
		},
		{
			"name": "BeeInstaller",
			"packageName": "bee",
			"type": "Class"
		},
		{
			"name": "Task",
			"packageName": "bee",
			"type": "Interface"
		},
		{
			"name": "Task.ValuedTaskReference",
			"packageName": "bee",
			"type": "Functional"
		},
		{
			"name": "Task.TaskReference",
			"packageName": "bee",
			"type": "Functional"
		},
		{
			"name": "BeeOption",
			"packageName": "bee",
			"type": "Class"
		},
		{
			"name": "Isolation",
			"packageName": "bee",
			"type": "AbstractClass"
		},
		{
			"name": "Intellij",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Install",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Install.TemporaryProject",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "IDE",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "License",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "License.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "FindMain",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "FindMain.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "FindMain.Search",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Dependency",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Compile",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Compile.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Github",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Jar",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Jar.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Clean",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Bun",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Wrapper",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Wrapper.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Release",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Release.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Deploy",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Native",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Native.Item",
			"packageName": "bee.task",
			"type": "Record"
		},
		{
			"name": "Native.Serialization",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Native.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Prototype",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "CI",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "JDK",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Jitpack",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Jitpack.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Maven",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Doc",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Doc.Listener",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "AnnotationProcessor",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Exe",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Exe.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Eclipse",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Eclipse.LombokInstaller",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Test",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "Test.Junit",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "Test.Config",
			"packageName": "bee.task",
			"type": "Class"
		},
		{
			"name": "AnnotationValidator",
			"packageName": "bee.task",
			"type": "AbstractClass"
		},
		{
			"name": "IDESupport",
			"packageName": "bee.task",
			"type": "Interface"
		},
		{
			"name": "PriorityClassLoader",
			"packageName": "bee",
			"type": "Class"
		},
		{
			"name": "Platform",
			"packageName": "bee",
			"type": "Class"
		},
		{
			"name": "Fail",
			"packageName": "bee",
			"type": "Exception"
		},
		{
			"name": "Help",
			"packageName": "bee",
			"type": "Interface"
		},
		{
			"name": "HeaderStyle",
			"packageName": "bee.coder",
			"type": "Interface"
		},
		{
			"name": "FileType",
			"packageName": "bee.coder",
			"type": "Interface"
		},
		{
			"name": "StandardHeaderStyle",
			"packageName": "bee.coder",
			"type": "Enum"
		}
	]
}