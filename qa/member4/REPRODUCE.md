# Reproducing the Member 4 evidence

Run from the repository root on an isolated Linux workstation. The run used Java 21, Maven Wrapper 3.9.16, JMeter 5.6.3, JaCoCo 0.8.13, SonarQube Community Build 25.3.0.104237 and SonarScanner CLI 6.2.1.4610. Use a fresh evidence directory for each rerun.

## Backend suite and coverage

In project/backend, run the wrapper. The first command resolves the test dependencies. Mockito is preloaded because this environment restricts JVM self-attachment.

~~~bash
sh mvnw test-compile
mock=$(find "$HOME/.m2/repository/org/mockito/mockito-core/5.17.0" -name '*.jar' | head -1)
sh mvnw org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent
jac=$(find "$HOME/.m2/repository/org/jacoco/org.jacoco.agent/0.8.13" -name '*runtime.jar' | head -1)
sh mvnw "-DargLine=-javaagent:$mock -javaagent:$jac=destfile=target/member4.exec" test
sh mvnw org.jacoco:jacoco-maven-plugin:0.8.13:report -Djacoco.dataFile=target/member4.exec
~~~

## Service, security and load

Use the application main class with its test classpath to make the H2 driver available. H2 uses an empty in-memory database and Hibernate creates the schema. This fixture exercises the real Spring HTTP and security layers. It does not exercise production Flyway migrations.

~~~bash
sh mvnw dependency:build-classpath -Dmdep.outputFile=target/qa-classpath.txt
java -cp "target/classes:$(cat target/qa-classpath.txt)" lk.publiccare.backend.PublicCareBackendApplication --server.address=127.0.0.1 '--spring.datasource.url=jdbc:h2:mem:qa;MODE=PostgreSQL' --spring.datasource.username=sa --spring.datasource.password= --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create
~~~

In a second terminal at repository root, issue ordinary anonymous GET requests. Run the load plan once health returns UP. Its 10-second scheduler cap bounds the five threads; each performs at most 20 requests, with a two-second ramp-up and a 250 ms timer.

~~~bash
for path in /api/health /api/admin/reports /api/auth/me; do
  curl -sS -w ' HTTP %{http_code}\n' "http://127.0.0.1:8080$path"
done
jmeter -n -t qa/member4/health.jmx -l fresh-health.jtl -j fresh-jmeter.log
~~~

## Browser checks

In project, run npm ci and npm run dev -- --host 127.0.0.1. Make Playwright available to Node, then from the root run the scripts below. CHROMIUM_PATH optionally selects an installed Chromium executable. Registration verifies browser form submission eligibility and intercepts the final submit event before an account can be created. The mobile walkthrough supplies a synthetic HTTP 401 session response.

~~~bash
SCREENSHOT=registration-retest.png node qa/member4/registration.cjs
node qa/member4/usability.cjs
~~~

## SonarQube

Start an isolated Community Build server on loopback. From project/backend, copy the test-scope dependencies so the Java analyzer can resolve imported types. From repository root, run the scanner with a token provided in SONAR_TOKEN and SONAR_HOST_URL pointing to the local server.

~~~bash
sh mvnw dependency:copy-dependencies -DincludeScope=test -DoutputDirectory=target/qa-libs
cd ../..
sonar-scanner -Dproject.settings=qa/member4/sonar-project.properties -Dsonar.scanner.skipJreProvisioning=true -Dsonar.qualitygate.wait=true
~~~

The scan imports JaCoCo XML from the backend. The checked-in JSON is the server measures response and a selected-field export of issues. The JTL, JaCoCo CSV, browser assertions and server exports allow independent checking of the report.
