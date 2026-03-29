<template>
  <div class="selection">
    <el-card class="card">
      <h3>选址平台查询</h3>
      <div class="filters">
        <el-input v-model="search.keyword" placeholder="关键字" clearable />
        <el-input v-model="search.levelMax" placeholder="最大层级(默认10)" clearable />
        <el-input v-model="search.limit" placeholder="返回条数(默认50)" clearable />
        <el-button type="primary" @click="fetchSearch">查询</el-button>
      </div>
      <el-table :data="searchList" border size="small" class="table-card">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fullName" label="标准地址" />
        <el-table-column prop="name" label="当级名称" />
        <el-table-column prop="level" label="层级" width="70" />
      </el-table>
    </el-card>

    <el-card class="card">
      <h3>新增房间地址</h3>
      <el-form :model="room" label-width="140px">
        <el-form-item label="楼栋标准地址ID">
          <el-input v-model="room.parentId" placeholder="楼栋标准地址ID" />
        </el-form-item>
        <el-form-item label="房间号/名称">
          <el-input v-model="room.roomName" placeholder="例如 1201" />
        </el-form-item>
        <el-form-item label="安装位置描述">
          <el-input v-model="room.installName" placeholder="为空默认使用房间号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitCreateRoomStandardAddress">创建房间地址</el-button>
        </el-form-item>
      </el-form>
      <div v-if="createResult" class="result">
        新标准地址ID：{{ createResult.standardAddressId }}，安装地址ID：{{ createResult.installationAddressId }}
      </div>
    </el-card>
  </div>
</template>

<script>
import { searchSelectionStandardAddresses, createRoomStandardAddress } from '../api/address';

export default {
  data() {
    return {
      search: {
        keyword: '',
        levelMax: '',
        limit: ''
      },
      searchList: [],
      room: {
        parentId: '',
        roomName: '',
        installName: ''
      },
      createResult: null
    };
  },
  methods: {
    async fetchSearch() {
      const params = {};
      if (this.search.keyword) params.keyword = this.search.keyword;
      if (this.search.levelMax) params.levelMax = Number(this.search.levelMax);
      if (this.search.limit) params.limit = Number(this.search.limit);
      const res = await searchSelectionStandardAddresses(params);
      this.searchList = res.data || [];
    },
    async submitCreateRoomStandardAddress() {
      const parentId = Number(this.room.parentId);
      if (!parentId || !this.room.roomName) {
        this.$message.warning('请填写完整参数');
        return;
      }
      const res = await createRoomStandardAddress({
        parentId,
        roomName: this.room.roomName,
        installName: this.room.installName || undefined
      });
      this.createResult = res.data;
      this.$message.success('房间地址创建成功');
    }
  }
};
</script>

<style scoped>
.selection {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.table-card {
  margin-top: 8px;
}
.card {
  padding-bottom: 4px;
  border-radius: 18px;
}
.result {
  margin-top: 8px;
  color: #409eff;
}
</style>
