import { useEffect, useId, useRef, useState } from 'react'

// A text input with a suggestion list (the ARIA combobox pattern): type, then pick with
// the mouse or arrow keys + Enter. Typing freely is always allowed too.
// search(query, signal) returns options; render(option) is the row; onPick(option) fills the form.
export default function Autocomplete({ value, onChange, onPick, search, render, minLength = 3, ...inputProps }) {
  const listId = useId()
  const [options, setOptions] = useState([])
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const typed = useRef(false) // only search after the user types, not when the form fills itself in

  useEffect(() => {
    if (!typed.current || value.trim().length < minLength) {
      setOptions([])
      return
    }
    // wait for a pause in typing, and cancel the request if they keep going
    const controller = new AbortController()
    const timer = setTimeout(() => {
      search(value.trim(), controller.signal)
        .then((found) => {
          setOptions(found)
          setActive(-1)
          setOpen(true)
        })
        .catch(() => {}) // aborted or offline: no suggestions, typing still works
    }, 350)
    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [value, minLength, search])

  function pick(option) {
    typed.current = false
    setOpen(false)
    setOptions([])
    onPick(option)
  }

  function onKeyDown(e) {
    if (!open || options.length === 0) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActive((i) => (i + 1) % options.length)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActive((i) => (i <= 0 ? options.length - 1 : i - 1))
    } else if (e.key === 'Enter' && active >= 0) {
      e.preventDefault() // pick, don't submit the form
      pick(options[active])
    } else if (e.key === 'Escape') {
      e.stopPropagation() // close the list, not the whole form
      setOpen(false)
    }
  }

  const showList = open && options.length > 0

  return (
    <div className="autocomplete">
      <input
        {...inputProps}
        value={value}
        onChange={(e) => {
          typed.current = true
          onChange(e.target.value)
        }}
        onKeyDown={onKeyDown}
        onBlur={() => setOpen(false)}
        onFocus={() => options.length && setOpen(true)}
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={showList}
        aria-controls={listId}
        aria-activedescendant={showList && active >= 0 ? `${listId}-${active}` : undefined}
        autoComplete="off"
      />
      {showList && (
        <ul className="suggestions" id={listId} role="listbox">
          {options.map((o, i) => (
            <li
              key={o.id}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === active}
              className={i === active ? 'active' : ''}
              // mousedown, not click: it fires before the input's blur closes the list
              onMouseDown={(e) => {
                e.preventDefault()
                pick(o)
              }}
            >
              {render(o)}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
