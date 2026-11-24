Component({
  properties: {
    text: { type: String, value: '暂无数据' },
    sub: { type: String, value: '' },
    action: { type: String, value: '' }
  },
  methods: {
    onTap() {
      this.triggerEvent('action')
    }
  }
})
