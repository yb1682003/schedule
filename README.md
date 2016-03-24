# schedule
主要解决分布式环境当中quartz的调度不适合多份部署的影响<br/>
当前项目只支持同一时间保证只有一份job会被调度<br/>

目前项目支持到热部署，实时修改项目的job调度时间等<br/>
<br/>

EXAMPLE:
        <dependency>
            <groupId>com.yili.schedule</groupId>
            <artifactId>schedule-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>


spring.xml

