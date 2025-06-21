# HintForge

**HintForge** is an Eclipse plug-in that provides intelligent assistants for Domain-Specific Languages (DSLs) defined using Xtext. By integrating Large Language Models (LLMs), it enables contextual code suggestions, identifier naming, and natural language explanations to support developers working with DSLs.

---

## Features

* **Context-Aware Code Completion**: Generate DSL code snippets guided by user instructions and code context.
* **Identifier Suggestions**: Propose semantically relevant names for new or existing elements.
* **Code Explanation**: Generate readable natural language summaries of code fragments.
* **Highly Configurable**: Support for multiple DSLs through customizable profiles.
* **Integrated with Eclipse**: Context menu actions, keyboard shortcuts, annotations, and a custom view.

---

## Getting Started

### Prerequisites

* Java 17+
* Eclipse IDE with Xtext support
* Internet access for LLM API usage

### Installation

1. Clone or download this repository.
2. Import as an Eclipse Plug-in Project.
3. Run the plug-in in a new Eclipse runtime instance.

### Usage

1. Open a DSL file in Eclipse (must match a configured profile).
2. Use the **HintForge** context menu or shortcuts:

   * `Ctrl+Alt+1` → Code Completion
   * `Ctrl+Alt+2` → Identifier Suggestion
   * `Ctrl+Alt+2` → Code Explanation
3. Review and accept suggestions directly in the editor.

---

## Configuration

Go to: `Window > Preferences > HintForge`

### DSL Profile Setup

Each profile includes:

* Name and description
* Path to `.xtext` grammar file
* File extension associated with the DSL
* Fully Qualified Class Name of the `StandaloneSetup`
* Optional: DSL examples, documentation, prompt customization

### LLM Settings

* Set your OpenAI API key
* Select model (e.g., `gpt-4o-mini`)

---

## Author

Developed by **Alejandro Ibáñez Pastrana**
Grado en Ingeniería Informática – Universidad Autónoma de Madrid
TFG: *Asistentes conversacionales para lenguajes de dominio específico* (2025)

