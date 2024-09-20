<!-- common 模块的 pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>common-module</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <local.jar.groupId>com.example</local.jar.groupId>
        <local.jar.artifactId>local-library</local.jar.artifactId>
        <local.jar.version>1.0</local.jar.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>${local.jar.groupId}</groupId>
            <artifactId>${local.jar.artifactId}</artifactId>
            <version>${local.jar.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-install-plugin</artifactId>
                <version>2.5.2</version>
                <executions>
                    <execution>
                        <id>install-local-jar</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>install-file</goal>
                        </goals>
                        <configuration>
                            <file>${project.basedir}/lib/local-library.jar</file>
                            <groupId>${local.jar.groupId}</groupId>
                            <artifactId>${local.jar.artifactId}</artifactId>
                            <version>${local.jar.version}</version>
                            <packaging>jar</packaging>
                            <generatePom>true</generatePom>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>

<!-- 子项目的 pom.xml -->
<project>
    <!-- ... 其他配置 ... -->
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-module</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>