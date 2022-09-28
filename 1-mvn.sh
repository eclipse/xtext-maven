mvn \
            -f org.eclipse.xtext.maven.parent/pom.xml \
            --batch-mode \
            --update-snapshots \
            -fae \
            -PuseJenkinsSnapshots \
            -Dmaven.test.failure.ignore=true \
            -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
            clean verify
