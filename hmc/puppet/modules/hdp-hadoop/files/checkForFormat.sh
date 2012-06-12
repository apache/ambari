#!/bin/sh

export hdfs_user=$1
shift
export conf_dir=$1
shift
export mark_file=$1
shift
export name_dirs=$*

export EXIT_CODE=0
export command="namenode -format"
export list_of_non_empty_dirs=""

if [[ ! -f $mark_file ]] ; then 
  for dir in `echo $name_dirs | tr ',' ' '` ; do
    echo "DIrname = $dir"
    cmd="ls $dir | wc -l  | grep -q ^0$"
    eval $cmd
    if [[ $? -ne 0 ]] ; then
      (( EXIT_CODE = $EXIT_CODE + 1 ))
      list_of_non_empty_dirs="$list_of_non_empty_dirs $dir"
    fi
  done

  if [[ $EXIT_CODE == 0 ]] ; then 
    su - ${hdfs_user} -c "yes Y | hadoop --config ${conf_dir} ${command}"
  else
    echo "ERROR: Namenode directory(s) is non empty. Will not format the namenode. List of non-empty namenode dirs ${list_of_non_empty_dirs}"
  fi
fi

exit $EXIT_CODE

