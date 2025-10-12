rm -r -f output/*
mvn clean package
echo "打包成功,路径: $(pwd)/output"
ls -l output/