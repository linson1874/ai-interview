import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import InterviewSetup from './InterviewSetup.vue'
import { exampleResumes } from './resume'

function mountSetup() {
  return mount(InterviewSetup, {
    props: { initialResume: exampleResumes[0]!, resumes: exampleResumes },
    global: { plugins: [ElementPlus] },
  })
}

function button(wrapper: ReturnType<typeof mountSetup>, label: string) {
  const result = wrapper.findAll('button').find((item) => item.text() === label)
  if (!result) throw new Error(`未找到按钮 ${label}`)
  return result
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  window.history.replaceState(null, '', '/')
})

afterEach(() => {
  vi.restoreAllMocks()
  vi.useRealTimers()
})

describe('面试准备', () => {
  it('从准备页开始面试后，重新加载仍进入本场作答页', async () => {
    window.history.replaceState(null, '', '/?report=example')
    let wrapper = mount(App, { global: { plugins: [ElementPlus] } })
    try {
      await flushPromises()
      await button(wrapper, '去模拟面试').trigger('click')
      await wrapper.get('.interview-form').trigger('submit')
      await vi.advanceTimersByTimeAsync(5000)
      await button(wrapper, '开始面试').trigger('click')
      expect(window.location.hash).toBe('#answer')
      expect(wrapper.get('.workspace-shell').isVisible()).toBe(false)

      wrapper.unmount()
      wrapper = mount(App, { global: { plugins: [ElementPlus] } })
      await flushPromises()
      expect(wrapper.get('.workspace-shell').isVisible()).toBe(false)
      expect(document.querySelector('.answer-topbar h1')?.textContent).toContain('Java 后端开发实习生')
      expect(document.querySelector('.exchange-current')?.textContent).toContain('请从简历选一项')
    } finally {
      wrapper.unmount()
    }
  })

  it('返回后旧生成任务不会入场，重新确认保留原配置', async () => {
    const wrapper = mountSetup()
    await wrapper.get('form').trigger('submit')
    expect(wrapper.get('[role="progressbar"]').exists()).toBe(true)
    expect(wrapper.get('.interview-pass').text()).toContain('Java 后端开发实习生')
    expect(wrapper.get('.step-complete').text()).toContain('创建面试会话')
    expect(wrapper.get('[aria-current="step"]').text()).toContain('准备「Java 后端开发实习生」面试内容')
    expect(button(wrapper, '开始面试').attributes('disabled')).toBeDefined()
    await vi.advanceTimersByTimeAsync(2000)
    expect(wrapper.get('[aria-current="step"]').text()).toContain('生成首轮面试题')
    await button(wrapper, '返回修改').trigger('click')
    await vi.advanceTimersByTimeAsync(5000)
    expect(wrapper.get('form').text()).toContain('Java 后端开发实习生')
    expect(wrapper.text()).not.toContain('题目已就绪')

    await wrapper.get('form').trigger('submit')
    expect(button(wrapper, '开始面试').attributes('disabled')).toBeDefined()
    await vi.advanceTimersByTimeAsync(5000)
    expect(wrapper.text()).toContain('题目已就绪')
    expect(wrapper.findAll('.step-complete')).toHaveLength(4)
    expect(wrapper.text()).not.toContain('麦克风检查')
    await button(wrapper, '开始面试').trigger('click')
    expect(wrapper.emitted('enter')?.[0]?.[0]).toMatchObject({ role: 'Java 后端开发实习生', format: '文字' })
    wrapper.unmount()
  })

  it('生成失败后可以保留配置重试', async () => {
    window.history.replaceState(null, '', '/?prepare=fail-once')
    const wrapper = mountSetup()
    await wrapper.get('form').trigger('submit')
    await vi.advanceTimersByTimeAsync(5000)
    expect(wrapper.get('[role="alert"]').text()).toContain('题目生成失败')
    expect(wrapper.get('.step-failed').text()).toContain('生成首轮面试题')
    await button(wrapper, '重试生成').trigger('click')
    expect(wrapper.text()).toContain('陈嘉宁_Java后端.pdf')
    await vi.advanceTimersByTimeAsync(5000)
    expect(wrapper.text()).toContain('题目已就绪')
    wrapper.unmount()
  })

  it('语音场次通过麦克风检查后才能开始，并释放测试音轨', async () => {
    const stop = vi.fn()
    const track = { readyState: 'live', stop }
    const getUserMedia = vi.fn()
      .mockRejectedValueOnce(new DOMException('Permission denied', 'NotAllowedError'))
      .mockResolvedValue({ getAudioTracks: () => [track], getTracks: () => [track] })
    const mediaDevices = Object.getOwnPropertyDescriptor(window.navigator, 'mediaDevices')
    Object.defineProperty(window.navigator, 'mediaDevices', { configurable: true, value: { getUserMedia } })
    const wrapper = mountSetup()
    try {
      await wrapper.get('input[type="radio"][value="语音"]').setValue(true)
      await wrapper.get('form').trigger('submit')
      expect(wrapper.text()).toContain('麦克风检查')
      expect(getUserMedia).not.toHaveBeenCalled()
      await vi.advanceTimersByTimeAsync(5000)
      expect(button(wrapper, '开始面试').attributes('disabled')).toBeDefined()
      expect(wrapper.get('[aria-current="step"]').text()).toContain('待检查')

      await button(wrapper, '检查麦克风').trigger('click')
      await flushPromises()
      expect(wrapper.get('[role="alert"]').text()).toContain('麦克风权限未开启')
      await button(wrapper, '重新检查').trigger('click')
      await flushPromises()
      expect(wrapper.text()).toContain('麦克风可用')
      expect(wrapper.findAll('.step-complete')).toHaveLength(4)
      expect(stop).toHaveBeenCalledOnce()
      expect(button(wrapper, '开始面试').attributes('disabled')).toBeUndefined()
    } finally {
      wrapper.unmount()
      if (mediaDevices) Object.defineProperty(window.navigator, 'mediaDevices', mediaDevices)
      else Reflect.deleteProperty(window.navigator, 'mediaDevices')
    }
  })
})
