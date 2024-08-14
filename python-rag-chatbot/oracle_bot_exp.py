import streamlit as st
import traceback
import sys
from init_rag_streamlit_exp import initialize_rag_chain, get_answer
from streamlit_feedback import streamlit_feedback

def process_feedback(feedback_value):
    st.write("Processing feedback value:", feedback_value)  # Debugging output
    try:
        with open("feedback.txt", "a", encoding="utf-8") as f:
            f.write(f"{feedback_value}\n")
        st.write("Feedback successfully written to file.")  # Debugging output
    except Exception as e:
        st.error(f"Error writing to file: {e}")
        traceback.print_exc()

def reset_conversation():
    st.session_state.messages = []
    st.session_state.feedback_rendered = False
    st.session_state.feedback_key = 0

st.title("Developing an AI bot powered by RAG and Oracle Database")

# Added reset button
st.button("Clear Chat History", on_click=reset_conversation)

# Initialize chat history
if "messages" not in st.session_state:
    reset_conversation()

# init RAG
rag_chain = initialize_rag_chain()

# Display chat messages from history on app rerun
for message in st.session_state.messages:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])

# React to user input
if question := st.chat_input("Hello, how can I help you?"):
    # Display user message in chat message container
    st.chat_message("user").markdown(question)
    # Add user message to chat history
    st.session_state.messages.append({"role": "user", "content": question})

    try:
        response = get_answer(rag_chain, question)

        with st.chat_message("assistant"):
            st.markdown(response)

            with st.container():
                st.markdown("How was my response?")

                if not st.session_state.feedback_rendered:
                    def _submit_feedback(feedback_value, *args, **kwargs):
                        print("Feedback submitted:", feedback_value, file=sys.stderr)  # Redirect to stderr
                        st.write("Feedback value received for submission:", feedback_value)  # Debugging output
                        process_feedback(feedback_value)
                        st.session_state.feedback_rendered = False

                    feedback_component = streamlit_feedback(
                        feedback_type="faces",
                        on_submit=_submit_feedback,
                        key=f"feedback_{st.session_state.feedback_key}",
                        optional_text_label="Please provide some more information",
                        args=["✅"]
                    )
                    st.session_state.feedback_key += 1
                    st.session_state.feedback_rendered = True

        st.session_state.messages.append({"role": "assistant", "content": response})

    except Exception as e:
        st.error(f"An error occurred: {e}")
        traceback.print_exc()
