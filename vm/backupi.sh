logger() {
    LOG_TYPE=$1
    MSG=$2

    TIME=$(date +%F" "%H:%M:%S)
    if [[ "${LOG_TYPE}" == "scd" ]]; then
        echo -e "SCDEngine:${MSG}"
    else
        echo -e "${TIME} -- ${LOG_TYPE}: ${MSG}"
    fi
}

VMWARE_CMD="vim-cmd"
VM_ID=37
SNAPSHOT_NAME1=`date +"%Y_%m_%d-%H:%M:%S"`"_FIRST"
SNAPSHOT_NAME2=`date +"%Y_%m_%d-%H:%M:%S"`"_SECOND"
SNAPSHOT_DESCRIPTION="Snapshot for scdEngine, deleted later on."
VM_SNAPSHOT_MEMORY=0
VM_SNAPSHOT_QUIESCE=0
VMX_DIR=/vmfs/volumes/datastore1/UbtJun
VM_NAME=UbtJun
VM_BACKUP=/vmfs/volumes/datastore1/backups/test
_p_w_d=$(pwd)

while getopts ":i:m:d:n:b:" ARGS; do
    case $ARGS in
        i)
            VM_ID=${OPTARG}
            ;;
        m)
            VM_SNAPSHOT_MEMORY=${OPTARG}
            ;;
        d)
            VMX_DIR=${OPTARG}
            ;;
        n)
            VM_NAME=${OPTARG}
            ;;
        b)
            VM_BACKUP=${OPTARG}
            ;;
        *)
            ;;

    esac
done


if [[ -f /usr/bin/vmware-vim-cmd ]]; then
    VMWARE_CMD=/usr/bin/vmware-vim-cmd
    VMKFSTOOLS_CMD=/usr/sbin/vmkfstools
    elif [[ -f /bin/vim-cmd ]]; then
        VMWARE_CMD=/bin/vim-cmd
        VMKFSTOOLS_CMD=/sbin/vmkfstools
    else
        logger "info" "ERROR: Unable to locate *vimsh*! You're not running ESX(i) 3.5+, 4.x+, 5.x+ or 6.x!"
        echo "ERROR: Unable to locate *vimsh*! You're not running ESX(i) 3.5+, 4.x+, 5.x+ or 6.x!"
        exit 1
fi

if [[ ! -d "${VM_BACKUP}" ]] ; then
    mkdir -p "${VM_BACKUP}"
    if [[ ! -d "${VM_BACKUP}" ]] ; then
        logger "info" "Unable to create \"${VM_BACKUP}\"! - Ensure VM_BACKUP_VOLUME was defined correctly"
        exit 1
    fi
fi

#创建第一次快照
logger "info" "Taking 1st snapshot..."
${VMWARE_CMD} vmsvc/snapshot.create ${VM_ID} ${SNAPSHOT_NAME1} "${SNAPSHOT_DESCRIPTION}" ${VM_SNAPSHOT_MEMORY} ${VM_SNAPSHOT_QUIESCE} > /dev/null 2>&1

#获取快照的序号
VMSD_PATH=`ls ${VMX_DIR}/*.vmsd`
SNAPSHOT_ID=`tail -n 1 "${VMSD_PATH}" | sed s/[[:space:]]//g | awk -F= '{print $2}' | sed s/\"//g`

#cp -f `ls -t ${VMX_DIR} | grep -v \`printf "%06d.vmdk" ${SNAPSHOT_ID}\` | grep -v \`printf "%06d-delta.vmdk" ${SNAPSHOT_ID}\` | grep -vE "*.vswp"` ${VM_BACKUP}
logger "scd" "+"
ls -t ${VMX_DIR} | grep -v `printf "%06d.vmdk" ${SNAPSHOT_ID}` | grep -v `printf "%06d-delta.vmdk" ${SNAPSHOT_ID}` | grep -vE "*.vswp"
logger "scd" "-"
read pause_cmd

#创建第二次快照
logger "info" "Taking 2nd snapshot..."
${VMWARE_CMD} vmsvc/snapshot.create ${VM_ID} ${SNAPSHOT_NAME2} "${SNAPSHOT_DESCRIPTION}" 0 0 > /dev/null 2>&1

#cp -f `ls -t ${VMX_DIR} | grep -E \`printf %06d.vmdk\\\|%06d-delta.vmdk ${SNAPSHOT_ID} ${SNAPSHOT_ID}\`` ${VM_BACKUP}
logger "scd" "+"
ls -t ${VMX_DIR} | grep -E `printf %06d.vmdk\\|%06d-delta.vmdk ${SNAPSHOT_ID} ${SNAPSHOT_ID}`
logger "scd" "-"
read pause_cmd

#删掉第二次做的快照
logger "info" "Deleting 2nd snapshot..."
SNAPSHOT_IDX=$(${VMWARE_CMD} vmsvc/snapshot.get ${VM_ID} | grep -E '(Snapshot Name|Snapshot Id)' | grep -A1 ${SNAPSHOT_NAME2} | grep "Snapshot Id" | awk -F ":" '{print $2}' | sed -e 's/^[[:blank:]]*//;s/[[:blank:]]*$//')
${VMWARE_CMD} vmsvc/snapshot.remove ${VM_ID} ${SNAPSHOT_IDX} > /dev/null 2>&1

#删掉第一次做的快照
logger "info" "Deleting 1st snapshot..."
SNAPSHOT_IDX=$(${VMWARE_CMD} vmsvc/snapshot.get ${VM_ID} | grep -E '(Snapshot Name|Snapshot Id)' | grep -A1 ${SNAPSHOT_NAME1} | grep "Snapshot Id" | awk -F ":" '{print $2}' | sed -e 's/^[[:blank:]]*//;s/[[:blank:]]*$//')
logger "scd" "snapshot_id1"
echo ${SNAPSHOT_IDX}
${VMWARE_CMD} vmsvc/snapshot.remove ${VM_ID} ${SNAPSHOT_IDX} > /dev/null 2>&1

#自删
cd $_p_w_d
rm $0
