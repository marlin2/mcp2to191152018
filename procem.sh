#!/bin/bash

if [ "$#" -ne 2 ]
then
  echo "Usage: procem.sh <dump_directory> <machine_name>"
  echo "See https://confluence.csiro.au/display/DataCentreShared/Converting+marlin2+records" 
  exit 1
fi
machine=$2

for i in $1/*
do
  if [ -f $i/info.xml ]
  then
     export isMCP=`grep '<schema>iso19139.mcp</schema>' $i/info.xml | wc -l`
     export isMCP14=`grep '<schema>iso19139.mcp-1.4</schema>' $i/info.xml | wc -l`
     export is19135=`grep '<schema>iso19135</schema>' $i/info.xml | wc -l`
     export isSDN=`grep '<schema>iso19139.sdn-csr</schema>' $i/info.xml | wc -l`
     if [ $isMCP -eq "1" ]
     then
       echo $i
       sed -i s/iso19139.mcp/iso19115-3/ $i/info.xml
       java -jar saxon.jar -s $i/metadata/metadata.xml -o $i/metadata/metadata.xml.new convert.xsl machine=$2
     else 
       echo rm -rf $i
       rm -rf $i
     fi 
  fi
done
java -jar target/mcp2to191152018-1.1.jar -d $1 -i metadata.xml.new -o metadata.xml.19115-3

