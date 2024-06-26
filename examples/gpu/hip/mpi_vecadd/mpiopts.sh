#!/bin/bash 

if [ "x$PE_MPICH_GTL_DIR_amd_gfx908" != "x" ]; then 
  MPI_EXTRA_OPTS="${PE_MPICH_GTL_DIR_amd_gfx908} ${PE_MPICH_GTL_LIBS_amd_gfx908} "
  mpicc_path=`which mpicc 2>/dev/null`
  if [ $? == 0  -a "x$mpicc_path" != "x" ]; then
    mpicc_path=""
    is_cray_mpi=`which mpicc | grep lmpi_cray | wc -l`
    if [ "x$is_cray_mpi" = "x0" ]; then
      MPI_EXTRA_OPTS=""
    fi
  else
    export PATH=${CRAY_MPICH_PREFIX}/bin:$PATH
  fi
fi

MPIOPTS=`mpicc -show | awk '{$1=""}1'  `
echo ${MPIOPTS}  ${MPI_EXTRA_OPTS}
  
