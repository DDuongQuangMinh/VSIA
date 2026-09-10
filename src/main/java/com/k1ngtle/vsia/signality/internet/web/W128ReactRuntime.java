package com.k1ngtle.vsia.signality.internet.web;

public final class W128ReactRuntime {
    private W128ReactRuntime() {
    }

    public static String source() {
        return """
                (() => {
                  const Fragment = Symbol('VSIA_FRAGMENT');
                  let currentRoot = null;
                  let currentHooks = null;
                  let hookIndex = 0;
                  let effectQueue = [];

                  const flat = input => {
                    const out = [];
                    const visit = value => {
                      if (Array.isArray(value)) value.forEach(visit);
                      else if (value !== null && value !== undefined && value !== false && value !== true) out.push(value);
                    };
                    input.forEach(visit);
                    return out;
                  };

                  const createElement = (type, props, ...children) => ({
                    type,
                    props: props || {},
                    children: flat(children)
                  });

                  const depsChanged = (before, after) => {
                    if (!before || !after || before.length !== after.length) return true;
                    for (let i = 0; i < before.length; i++) if (!Object.is(before[i], after[i])) return true;
                    return false;
                  };

                  const useState = initial => {
                    if (!currentHooks) throw new Error('useState must run inside a component');
                    const hooks = currentHooks;
                    const slot = hookIndex++;
                    if (!(slot in hooks)) hooks[slot] = typeof initial === 'function' ? initial() : initial;
                    const setState = next => {
                      hooks[slot] = typeof next === 'function' ? next(hooks[slot]) : next;
                      if (currentRoot) currentRoot.schedule();
                    };
                    return [hooks[slot], setState];
                  };

                  const useMemo = (factory, deps) => {
                    if (!currentHooks) throw new Error('useMemo must run inside a component');
                    const hooks = currentHooks;
                    const slot = hookIndex++;
                    const old = hooks[slot];
                    if (!old || depsChanged(old.deps, deps)) hooks[slot] = { value: factory(), deps };
                    return hooks[slot].value;
                  };

                  const useEffect = (effect, deps) => {
                    if (!currentHooks) throw new Error('useEffect must run inside a component');
                    const hooks = currentHooks;
                    const slot = hookIndex++;
                    const old = hooks[slot];
                    if (!old || depsChanged(old.deps, deps)) {
                      effectQueue.push(() => {
                        if (old && typeof old.cleanup === 'function') old.cleanup();
                        const cleanup = effect();
                        hooks[slot] = { deps, cleanup };
                      });
                    }
                  };

                  const setProp = (node, name, value) => {
                    if (name === 'children' || value === undefined || value === null || value === false) return;
                    if (name === 'className') {
                      node.setAttribute('class', String(value));
                      return;
                    }
                    if (name === 'htmlFor') {
                      node.setAttribute('for', String(value));
                      return;
                    }
                    if (name === 'style' && value && typeof value === 'object') {
                      Object.assign(node.style, value);
                      return;
                    }
                    if (name === 'dangerouslySetInnerHTML' && value && typeof value.__html === 'string') {
                      node.innerHTML = value.__html;
                      return;
                    }
                    if (/^on[A-Z]/.test(name) && typeof value === 'function') {
                      node.addEventListener(name.substring(2).toLowerCase(), value);
                      return;
                    }
                    if (name in node && !name.startsWith('data-') && !name.startsWith('aria-')) {
                      try {
                        node[name] = value;
                        return;
                      } catch (_) {
                      }
                    }
                    if (value === true) node.setAttribute(name, '');
                    else node.setAttribute(name, String(value));
                  };

                  const componentHooks = new WeakMap();

                  const renderNode = vnode => {
                    if (vnode === null || vnode === undefined || vnode === false || vnode === true) return document.createTextNode('');
                    if (typeof vnode === 'string' || typeof vnode === 'number') return document.createTextNode(String(vnode));
                    if (Array.isArray(vnode)) {
                      const fragment = document.createDocumentFragment();
                      vnode.forEach(child => fragment.appendChild(renderNode(child)));
                      return fragment;
                    }
                    if (vnode.type === Fragment) {
                      const fragment = document.createDocumentFragment();
                      vnode.children.forEach(child => fragment.appendChild(renderNode(child)));
                      return fragment;
                    }
                    if (typeof vnode.type === 'function') {
                      let hooks = componentHooks.get(vnode.type);
                      if (!hooks) {
                        hooks = [];
                        componentHooks.set(vnode.type, hooks);
                      }
                      const previousHooks = currentHooks;
                      const previousIndex = hookIndex;
                      currentHooks = hooks;
                      hookIndex = 0;
                      const rendered = vnode.type({ ...vnode.props, children: vnode.children });
                      currentHooks = previousHooks;
                      hookIndex = previousIndex;
                      return renderNode(rendered);
                    }
                    const node = document.createElement(vnode.type);
                    Object.entries(vnode.props || {}).forEach(([name, value]) => setProp(node, name, value));
                    if (!(vnode.props && vnode.props.dangerouslySetInnerHTML)) {
                      vnode.children.forEach(child => node.appendChild(renderNode(child)));
                    }
                    return node;
                  };

                  const createRoot = container => {
                    if (!container) throw new Error('createRoot requires a container');
                    const root = {
                      vnode: null,
                      queued: false,
                      render(vnode) {
                        this.vnode = vnode;
                        this.commit();
                      },
                      schedule() {
                        if (this.queued) return;
                        this.queued = true;
                        queueMicrotask(() => {
                          this.queued = false;
                          this.commit();
                        });
                      },
                      commit() {
                        currentRoot = this;
                        effectQueue = [];
                        container.replaceChildren(renderNode(this.vnode));
                        const pending = effectQueue.slice();
                        effectQueue = [];
                        pending.forEach(run => run());
                        currentRoot = this;
                      }
                    };
                    return root;
                  };

                  window.React = { createElement, Fragment, useState, useEffect, useMemo };
                  window.ReactDOM = { createRoot };
                })();
                """;
    }
}
