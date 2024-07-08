#
# Streamlit App to demo OCI AI GenAI
# this is the main code, with the UI
#
import streamlit as st

# this function initialise the rag chain, creating retriever, llm and chain
from init_rag_streamlit import initialize_rag_chain, get_answer

#
# Configs
#


def reset_conversation():
    st.session_state.messages = []


#
# Main
#
st.title("OSD Generative AI Bot powered by RAG")

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

    # here we call OCI genai...

    try:
        print("...Panks call")
        response = get_answer(rag_chain, question)
        print("...Panks call response"+response)
        # Display assistant response in chat message container
        with st.chat_message("assistant"):
            st.markdown(response)

        # Add assistant response to chat history
        st.session_state.messages.append({"role": "assistant", "content": response})

    except Exception as e:
        st.error("An error for the panks occurred: " + str(e))
