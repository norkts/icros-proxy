# build
``
./package.sh
``

# run
``
java -jar output/proxy-web-1.0-SNAPSHOT.jar
``

# proxy
before execute any request, replace url as http://127.0.0.1:8080/proxy/${url}

such as:
``
curl -i http://127.0.0.1:8080/proxy/https://www.baidu.com
``

support methods GET,POST,OPTIONS
