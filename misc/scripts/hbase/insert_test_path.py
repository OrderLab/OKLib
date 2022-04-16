import csv

with open('hbase-batch3.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    for row in csv_reader:
	file_name = 'recipes/'+row[0].replace(' ','')+'.properties'
	test_full_name = row[3].replace('.java','').replace('hbase-server/src/test/java/','').replace('src/test/java/','').replace('/','.')
	test_class_name = test_full_name.split('@')[0]
	f = open(file_name, 'a')  
	f.write('\ntest_name='+test_class_name)
	f.write('\ntest_trace_prefix='+test_full_name)
	f.close()
