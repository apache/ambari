;;; jinja.el --- Jinja mode highlighting
;;
;; Author: Georg Brandl
;; Copyright: (c) 2009 by the Jinja Team
;; Last modified: 2008-05-22 23:04 by gbr
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;; Commentary:
;;
;; Mostly ripped off django-mode by Lennart Borgman.
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; This program is free software; you can redistribute it and/or
;; modify it under the terms of the GNU General Public License as
;; published by the Free Software Foundation; either version 2, or
;; (at your option) any later version.
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with this program; see the file COPYING.  If not, write to
;; the Free Software Foundation, Inc., 51 Franklin Street, Fifth
;; Floor, Boston, MA 02110-1301, USA.
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;; Code:

(defconst jinja-font-lock-keywords
  (list
;   (cons (rx "{% comment %}" (submatch (0+ anything))
;             "{% endcomment %}") (list 1 font-lock-comment-face))
   '("{# ?\\(.*?\\) ?#}" . (1 font-lock-comment-face))
   '("{%-?\\|-?%}\\|{{\\|}}" . font-lock-preprocessor-face)
   '("{#\\|#}" . font-lock-comment-delimiter-face)
   ;; first word in a block is a command
   '("{%-?[ \t\n]*\\([a-zA-Z_]+\\)" . (1 font-lock-keyword-face))
   ;; variables
   '("\\({{ ?\\)\\([^|]*?\\)\\(|.*?\\)? ?}}" . (1 font-lock-variable-name-face))
   ;; keywords and builtins
   (cons (rx word-start
             (or "in" "as" "recursive" "not" "and" "or" "if" "else"
                 "import" "with" "without" "context")
             word-end)
         font-lock-keyword-face)
   (cons (rx word-start
             (or "true" "false" "none" "loop" "self" "super")
             word-end)
         font-lock-builtin-face)
   ;; tests
   '("\\(is\\)[ \t]*\\(not\\)[ \t]*\\([a-zA-Z_]+\\)"
     (1 font-lock-keyword-face) (2 font-lock-keyword-face)
     (3 font-lock-function-name-face))
   ;; builtin filters
   (cons (rx
          "|" (* space)
          (submatch
           (or "abs" "batch" "capitalize" "capture" "center" "count" "default"
               "dformat" "dictsort" "e" "escape" "filesizeformat" "first"
               "float" "format" "getattribute" "getitem" "groupby" "indent"
               "int" "join" "jsonencode" "last" "length" "lower" "markdown"
               "pprint" "random" "replace" "reverse" "round" "rst" "slice"
               "sort" "string" "striptags" "sum" "textile" "title" "trim"
               "truncate" "upper" "urlencode" "urlize" "wordcount" "wordwrap"
               "xmlattr")))
         (list 1 font-lock-builtin-face))
   )
   "Minimal highlighting expressions for Jinja mode")

(define-derived-mode jinja-mode nil "Jinja"
  "Simple Jinja mode for use with `mumamo-mode'.
This mode only provides syntax highlighting."
  ;;(set (make-local-variable 'comment-start) "{#")
  ;;(set (make-local-variable 'comment-end)   "#}")
  (setq font-lock-defaults '(jinja-font-lock-keywords)))

;; mumamo stuff

(when (require 'mumamo nil t)

  (defun mumamo-chunk-jinja3(pos min max)
    "Find {# ... #}.  Return range and `jinja-mode'.
See `mumamo-find-possible-chunk' for POS, MIN and MAX."
    (mumamo-find-possible-chunk pos min max
                                'mumamo-search-bw-exc-start-jinja3
                                'mumamo-search-bw-exc-end-jinja3
                                'mumamo-search-fw-exc-start-jinja3
                                'mumamo-search-fw-exc-end-jinja3))

  (defun mumamo-chunk-jinja2(pos min max)
    "Find {{ ... }}.  Return range and `jinja-mode'.
See `mumamo-find-possible-chunk' for POS, MIN and MAX."
    (mumamo-find-possible-chunk pos min max
                                'mumamo-search-bw-exc-start-jinja2
                                'mumamo-search-bw-exc-end-jinja2
                                'mumamo-search-fw-exc-start-jinja2
                                'mumamo-search-fw-exc-end-jinja2))

  (defun mumamo-chunk-jinja (pos min max)
    "Find {% ... %}.  Return range and `jinja-mode'.
See `mumamo-find-possible-chunk' for POS, MIN and MAX."
    (mumamo-find-possible-chunk pos min max
                                'mumamo-search-bw-exc-start-jinja
                                'mumamo-search-bw-exc-end-jinja
                                'mumamo-search-fw-exc-start-jinja
                                'mumamo-search-fw-exc-end-jinja))

  (defun mumamo-search-bw-exc-start-jinja (pos min)
    "Helper for `mumamo-chunk-jinja'.
POS is where to start search and MIN is where to stop."
    (let ((exc-start (mumamo-chunk-start-bw-str-inc pos min "{%")))
      (and exc-start
           (<= exc-start pos)
           (cons exc-start 'jinja-mode))))

  (defun mumamo-search-bw-exc-start-jinja2(pos min)
    "Helper for `mumamo-chunk-jinja2'.
POS is where to start search and MIN is where to stop."
    (let ((exc-start (mumamo-chunk-start-bw-str-inc pos min "{{")))
      (and exc-start
           (<= exc-start pos)
           (cons exc-start 'jinja-mode))))

  (defun mumamo-search-bw-exc-start-jinja3(pos min)
    "Helper for `mumamo-chunk-jinja3'.
POS is where to start search and MIN is where to stop."
    (let ((exc-start (mumamo-chunk-start-bw-str-inc pos min "{#")))
      (and exc-start
           (<= exc-start pos)
           (cons exc-start 'jinja-mode))))

  (defun mumamo-search-bw-exc-end-jinja (pos min)
    "Helper for `mumamo-chunk-jinja'.
POS is where to start search and MIN is where to stop."
    (mumamo-chunk-end-bw-str-inc pos min "%}"))

  (defun mumamo-search-bw-exc-end-jinja2(pos min)
    "Helper for `mumamo-chunk-jinja2'.
POS is where to start search and MIN is where to stop."
    (mumamo-chunk-end-bw-str-inc pos min "}}"))

  (defun mumamo-search-bw-exc-end-jinja3(pos min)
    "Helper for `mumamo-chunk-jinja3'.
POS is where to start search and MIN is where to stop."
    (mumamo-chunk-end-bw-str-inc pos min "#}"))

  (defun mumamo-search-fw-exc-start-jinja (pos max)
    "Helper for `mumamo-chunk-jinja'.
POS is where to start search and MAX is where to stop."
    (mumamo-chunk-start-fw-str-inc pos max "{%"))

  (defun mumamo-search-fw-exc-start-jinja2(pos max)
    "Helper for `mumamo-chunk-jinja2'.
POS is where to start search and MAX is where to stop."
    (mumamo-chunk-start-fw-str-inc pos max "{{"))

  (defun mumamo-search-fw-exc-start-jinja3(pos max)
    "Helper for `mumamo-chunk-jinja3'.
POS is where to start search and MAX is where to stop."
    (mumamo-chunk-start-fw-str-inc pos max "{#"))

  (defun mumamo-search-fw-exc-end-jinja (pos max)
    "Helper for `mumamo-chunk-jinja'.
POS is where to start search and MAX is where to stop."
    (mumamo-chunk-end-fw-str-inc pos max "%}"))

  (defun mumamo-search-fw-exc-end-jinja2(pos max)
    "Helper for `mumamo-chunk-jinja2'.
POS is where to start search and MAX is where to stop."
    (mumamo-chunk-end-fw-str-inc pos max "}}"))

  (defun mumamo-search-fw-exc-end-jinja3(pos max)
    "Helper for `mumamo-chunk-jinja3'.
POS is where to start search and MAX is where to stop."
    (mumamo-chunk-end-fw-str-inc pos max "#}"))

;;;###autoload
  (define-mumamo-multi-major-mode jinja-html-mumamo
    "Turn on multiple major modes for Jinja with main mode `html-mode'.
This also covers inlined style and javascript."
    ("Jinja HTML Family" html-mode
     (mumamo-chunk-jinja
      mumamo-chunk-jinja2
      mumamo-chunk-jinja3
      mumamo-chunk-inlined-style
      mumamo-chunk-inlined-script
      mumamo-chunk-style=
      mumamo-chunk-onjs=
      )))

;;;###autoload
  (define-mumamo-multi-major-mode jinja-nxhtml-mumamo
    "Turn on multiple major modes for Jinja with main mode `nxhtml-mode'.
This also covers inlined style and javascript."
    ("Jinja nXhtml Family" nxhtml-mode
     (mumamo-chunk-jinja
      mumamo-chunk-jinja2
      mumamo-chunk-jinja3
      mumamo-chunk-inlined-style
      mumamo-chunk-inlined-script
      mumamo-chunk-style=
      mumamo-chunk-onjs=
      )))
  )

(provide 'jinja)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; jinja.el ends here
