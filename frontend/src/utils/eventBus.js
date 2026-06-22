const listeners = new Map()

export const eventBus = {
  on(event, callback) {
    if (!listeners.has(event)) {
      listeners.set(event, new Set())
    }
    listeners.get(event).add(callback)
    return () => this.off(event, callback)
  },

  off(event, callback) {
    if (listeners.has(event)) {
      listeners.get(event).delete(callback)
    }
  },

  emit(event, data) {
    if (listeners.has(event)) {
      listeners.get(event).forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          console.error(`Event handler error for ${event}:`, e)
        }
      })
    }
  }
}

export const EVENTS = {
  STOPPER_SCRAPPED: 'stopper:scrapped',
  STOPPER_UPDATED: 'stopper:updated',
  STOPPER_SHIFTED: 'stopper:shifted'
}
