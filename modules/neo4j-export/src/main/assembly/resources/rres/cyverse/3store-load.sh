# TODO: comment me!
# TODO: Move these scripts to the backend repo

release="$1"; shift
datasets=${@:=arabidopsis wheat}

vutils_home=/opt/software/virtuoso-utils
data_dir=/opt/data
graph_uribase=http://knetminer.org/data/rdf/resources/graphs/

is_first='true'
for dataset in $datasets
do	
	echo -e "\n\n\tWorking on $dataset $release\n"
	cd "$data_dir"

  echo -e "\n--- Downloading RDF files\n"
  rm -f "tmp/${dataset}-rdf.tar.bz2"
  rclone rclone copy "brandizi_rres_onedrive:knetminer-pub-data/$release/$dataset/${dataset}-rdf.tar.bz2" tmp

  echo -e "\n--- Extracting RDF files\n"
  # This assumes all the tarballs have the same ontologies. 
  # TODO: more ontologies
	[ "$is_first" == "true" ] || rm -f rdf/ontologies 
  tar x --bzip2 -f "tmp/${dataset}-rdf.tar.bz2" rdf/ontologies

  # Virtuoso can load from .gz, because we don't have much space on the server, let's convert the original ttl
  # in a pipeline
  rm -f "rdf/$dataset/${dataset}.ttl.gz"
  tar x --to-stdout --bzip2 -f "tmp/${dataset}-rdf.tar.bz2" "rdf/${dataset}.ttl" \
    | gzip --stdout >"rdf/$dataset/${dataset}.ttl.gz"

  echo -e "\n--- Loading on Virtuoso\n"
  cd "$vutils_home"
  [ "$is_first" == "true" ] || ./virt_load.sh -r "$data_dir/ontologies" "${graph_uribase}ontologies"
  ./virt_load.sh "$data_dir/$dataset" "$graph_uribase$dataset" &
  
	is_first='false'
done

echo -e "\n\n\tWaiting for the completion of all Virtuoso loadings (use virt_checkpoint.sh meanwhile)\n"
wait $(jobs -p)

echo -e "\n\n\tThe End\n" 
