import os

directory = '/Users/McfateAlan/OathKeeper/HBASE_CACHE_CLASSPATH_DIR/log30_select'

for filename in os.listdir(directory):
    f_full = os.path.join(directory, filename)
    # checking if it is a file
    if not os.path.isfile(f_full):
        print(f_full)
	with open(f_full+'/cached_classpath.txt') as f:
		cl_path = f.readline()
		cl_path_parsed = (cl_path.replace("\n", " ").replace("/Users/McfateAlan", "${user_root}").replace("/home/chang", "${user_root}"))
		f2 = open('/Users/McfateAlan/OathKeeper/conf/samples/hb-collections-large-gen/'+filename+'.properties', 'a')  # open file in append mode
		f2.write('\n'+'java_class_path="'+cl_path_parsed+'"')
		f2.close()
'''
for filename in os.listdir(directory):
    if filename.endswith(".properties"):
	f = open(file_name, 'a')  # open file in append mode
	f.write('\n'+)
	f.close()


with open('hbase-batch3.csv') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    for row in csv_reader:
'''
